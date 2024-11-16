package com.weathersync.features.activityPlanning

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestClock
import com.weathersync.common.TestHelper
import com.weathersync.common.data.createInMemoryDataStore
import com.weathersync.common.utils.mockLimitManager
import com.weathersync.common.utils.mockLimitManagerFirestore
import com.weathersync.common.utils.mockSubscriptionManager
import com.weathersync.common.utils.mockTimeAPI
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.common.weather.locationInfo
import com.weathersync.common.weather.mockGenerativeModel
import com.weathersync.common.weather.mockLocationClient
import com.weathersync.common.weather.mockWeatherUnitsManager
import com.weathersync.features.activityPlanning.data.ForecastUnits
import com.weathersync.features.activityPlanning.data.Hourly
import com.weathersync.features.activityPlanning.data.OpenMeteoForecast
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.ads.AdsDatastoreManager
import com.weathersync.utils.ads.adsDataStore
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.weather.FirestoreWeatherUnit
import com.weathersync.utils.weather.limits.GenerationType
import com.weathersync.utils.weather.limits.LimitManager
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import com.weathersync.utils.weather.WeatherUnitsManager
import com.weathersync.utils.weather.limits.TimeAPIResponse
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.stopKoin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class ActivityPlanningBaseRule: TestWatcher() {
    val testHelper = TestHelper()
    val testClock = TestClock()
    val testDispatcher = StandardTestDispatcher()
    val premiumLimitManagerConfig = GenerationType.ActivityRecommendations.premiumLimitManagerConfig
    val regularLimitManagerConfig = GenerationType.ActivityRecommendations.regularLimitManagerConfig

    lateinit var viewModel: ActivityPlanningViewModel
    lateinit var limitManager: LimitManager
    val currentWeatherDAO: CurrentWeatherDAO = mockk()
    lateinit var limitManagerFirestore: FirebaseFirestore
    lateinit var generativeModel: GenerativeModel
    lateinit var geminiRepository: ActivityPlanningGeminiRepository
    lateinit var activityPlanningRepository: ActivityPlanningRepository
    lateinit var forecastRepository: ForecastRepository
    lateinit var weatherUnitsManager: WeatherUnitsManager
    lateinit var subscriptionInfoDatastore: SubscriptionInfoDatastore

    fun advance(testScope: TestScope) = repeat(9999999) { testScope.advanceUntilIdle() }
    private var capturedUrl = ""

    fun setupViewModel(locale: Locale = Locale.US) {
        viewModel = ActivityPlanningViewModel(
            activityPlanningRepository = activityPlanningRepository,
            analyticsManager = testHelper.getAnalyticsManager(
                AdsDatastoreManager(
                    ApplicationProvider.getApplicationContext<Context>().adsDataStore
                )
            ),
            nextUpdateTimeFormatter = NextUpdateTimeFormatter(
                clock = testClock,
                locale = locale
            ),
            subscriptionInfoDatastore = subscriptionInfoDatastore
        )
    }
    fun setupActivityPlanningRepository(
        isSubscribed: IsSubscribed,
        generatedSuggestions: String? = null,
        suggestionsGenerationException: Exception? = null
    ) {
        geminiRepository = spyk(mockGeminiRepository(
            generatedContent = generatedSuggestions,
            suggestionsGenerationException = suggestionsGenerationException
        ))
        activityPlanningRepository = spyk(ActivityPlanningRepository(
            limitManager = limitManager,
            subscriptionManager = mockSubscriptionManager(isSubscribed = isSubscribed),
            forecastRepository = forecastRepository,
            activityPlanningGeminiRepository = geminiRepository,
            dispatcher = testDispatcher
        ))
    }
    fun setupForecastRepository(
        units: List<WeatherUnit> = fetchedWeatherUnits,
        status: HttpStatusCode = HttpStatusCode.OK,
        geocoderException: Exception? = null,
        lastLocationException: Exception? = null
    ) {
        forecastRepository = spyk(ForecastRepository(
            engine = mockForecastEngine(status, units = units),
            locationClient = mockLocationClient(
                geocoderException = geocoderException,
                lastLocationException = lastLocationException
            ),
            weatherUnitsManager = weatherUnitsManager
        ))
    }
    fun setupWeatherUnitsManager(
        units: List<WeatherUnit> = fetchedWeatherUnits,
        unitsFetchException: Exception? = null,
        unitSetException: Exception? = null
    ) {
        weatherUnitsManager = mockWeatherUnitsManager(
            firestoreUnits = units.map { FirestoreWeatherUnit(it.unitName) },
            unitsFetchException = unitsFetchException,
            unitSetException = unitSetException
        )
    }
    fun setupLimitManager(
        timestamps: List<Timestamp> = listOf(),
        timeApiStatusCode: HttpStatusCode = HttpStatusCode.OK,
        exception: Exception? = null) {
        limitManagerFirestore = mockLimitManagerFirestore(
            timestamps = timestamps,
            exception = exception
        )
        limitManager = spyk(mockLimitManager(
            limitManagerFirestore = limitManagerFirestore,
            currentWeatherDAO = currentWeatherDAO,
            weatherUpdater = mockk(),
            timeAPI = mockTimeAPI(statusCode = timeApiStatusCode, currTimeMillis = testClock.millis())
        ))
    }

    private fun mockForecastEngine(
        status: HttpStatusCode,
        units: List<WeatherUnit>,
    ): HttpClientEngine = MockEngine { request ->
        capturedUrl = request.url.toString()
        val extractedTimes = extractTimes(capturedUrl)
        val mockedForecast = OpenMeteoForecast(
            forecastUnits = ForecastUnits(
                time = "hours",
                temp = units.first { it is WeatherUnit.Temperature }.unitName,
                humidity = "%",
                apparentTemp = "°C",
                windSpeed = units.first { it is WeatherUnit.WindSpeed }.unitName,
                precipProb = "%",
                weatherCode = "wmo code",
                visibility = units.first { it is WeatherUnit.Visibility }.unitName,
                pressure = "hPa"
            ),
            hourly = Hourly(
                time = listOf(extractedTimes.first, extractedTimes.second),
                temp = listOf(20.0, 22.0),
                humidity = listOf(50.0, 55.0),
                apparentTemp = listOf(22.0, 24.0),
                windSpeed = listOf(15.0, 18.0),
                precipProb = listOf(50.0, 55.0),
                weatherCode = listOf(2, 1),
                visibility = listOf(10.0, 4.9),
                pressure = listOf(1013.0, 981.3)
            ),
            locality = "${locationInfo.city}, ${locationInfo.country}"
        )
        val jsonResponse = Json.encodeToString(mockedForecast)
        respond(
            content = ByteReadChannel(jsonResponse),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    private fun mockGeminiRepository(
        generatedContent: String? = null,
        suggestionsGenerationException: Exception? = null
    ): ActivityPlanningGeminiRepository {
        generativeModel = mockGenerativeModel(generatedContent, generationException = suggestionsGenerationException)
        return ActivityPlanningGeminiRepository(
            generativeModel = generativeModel,
            is24HourFormat = true
        )
    }
    override fun starting(description: Description?) {
        stopKoin()
        unmockkAll()
        subscriptionInfoDatastore = SubscriptionInfoDatastore(
            dataStore = createInMemoryDataStore()
        )
        setupLimitManager()
        setupWeatherUnitsManager()
        setupForecastRepository()
        setupActivityPlanningRepository(
            isSubscribed = false,
            generatedSuggestions = generatedSuggestions)
        setupViewModel()
    }
    val activityPlanningSuggestions = "The best time to do that is October 3, 12:45, 2024"
    val generatedSuggestions = """
        $activitiesPlanningTag
        $activityPlanningSuggestions
        $activitiesPlanningTag
    """.trimIndent()

    fun assertUrlAndDatesAreCorrect(isSubscribed: IsSubscribed = false) {
        assertTrue(capturedUrl.isNotEmpty())
        val extractedTimes = extractTimes(capturedUrl)
        assertDateDifference(isSubscribed, extractedTimes.first, extractedTimes.second)
    }
    private fun extractTimes(url: String): Pair<String, String> {
        val startHourRegex = "start_hour=([^&]+)".toRegex()
        val endHourRegex = "end_hour=([^&]+)".toRegex()

        val startHour = startHourRegex.find(url)?.groupValues!![1]
        val endHour = endHourRegex.find(url)?.groupValues!![1]

        return Pair(startHour, endHour)
    }
    private fun assertDateDifference(
        isSubscribed: IsSubscribed,
        start: String, end: String) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val startDateTime = LocalDateTime.parse(start, formatter)
        val endDateTime = LocalDateTime.parse(end, formatter)
        val daysBetween = ChronoUnit.DAYS.between(startDateTime, endDateTime)
        assertEquals((if (isSubscribed) ForecastDays.PREMIUM else ForecastDays.REGULAR).days, daysBetween.toInt())
    }
}