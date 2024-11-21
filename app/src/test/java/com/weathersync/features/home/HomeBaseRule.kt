package com.weathersync.features.home

import android.Manifest
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestClock
import com.weathersync.common.TestException
import com.weathersync.common.TestHelper
import com.weathersync.common.data.createInMemoryDataStore
import com.weathersync.common.utils.ai.mockAIClientProvider
import com.weathersync.common.weather.mockEngine
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.common.weather.locationInfo
import com.weathersync.common.utils.mockLimitManager
import com.weathersync.common.utils.mockLimitManagerFirestore
import com.weathersync.common.utils.mockSubscriptionManager
import com.weathersync.common.utils.mockTimeAPI
import com.weathersync.common.weather.mockLocationClient
import com.weathersync.common.weather.mockWeatherUnitsManager
import com.weathersync.features.home.data.CurrWeather
import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.CurrentWeatherUnits
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.db.CurrentWeatherLocalDB
import com.weathersync.features.home.domain.CurrentWeatherRepository
import com.weathersync.features.home.domain.HomeAIRepository
import com.weathersync.features.home.domain.HomeRepository
import com.weathersync.features.home.domain.WeatherUpdater
import com.weathersync.features.home.presentation.HomeViewModel
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.ads.AdsDatastoreManager
import com.weathersync.utils.ai.AIClientProvider
import com.weathersync.utils.ai.gemini.data.GeminiCandidate
import com.weathersync.utils.ai.gemini.data.GeminiPart
import com.weathersync.utils.ai.gemini.data.GeminiParts
import com.weathersync.utils.ai.gemini.data.GeminiResponse
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.weather.FirestoreWeatherUnit
import com.weathersync.utils.weather.limits.GenerationType
import com.weathersync.utils.weather.limits.LimitManager
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import com.weathersync.utils.weather.WeatherUnitsManager
import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.stopKoin
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.util.Locale

class HomeBaseRule: TestWatcher() {
    val testHelper = TestHelper()
    val testClock = TestClock()
    val testDispatcher = StandardTestDispatcher()
    val premiumLimitManagerConfig = GenerationType.CurrentWeather(null).premiumLimitManagerConfig
    val regularLimitManagerConfig = GenerationType.CurrentWeather(null).regularLimitManagerConfig

    val crashlyticsExceptionSlot = testHelper.exceptionSlot
    val exception = TestException("exception")
    lateinit var analyticsManager: AnalyticsManager
    lateinit var viewModel: HomeViewModel
    fun advance(testScope: TestScope) = repeat(99999999) { testScope.advanceUntilIdle() }

    lateinit var weatherUpdater: WeatherUpdater
    private lateinit var currentWeatherRepository: CurrentWeatherRepository
    lateinit var limitManager: LimitManager
    lateinit var currentWeatherLocalDB: CurrentWeatherLocalDB
    lateinit var homeRepository: HomeRepository
    lateinit var aiClientProvider: AIClientProvider
    lateinit var limitManagerFirestore: FirebaseFirestore
    private lateinit var geminiRepository: HomeAIRepository
    lateinit var weatherUnitsManager: WeatherUnitsManager
    lateinit var subscriptionInfoDatastore: SubscriptionInfoDatastore

    fun manageLocationPermission(grant: Boolean) {
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        val shadowApp = Shadows.shadowOf(RuntimeEnvironment.getApplication())
        shadowApp.apply { if (grant) grantPermissions(permission) else denyPermissions(permission) }
    }

    fun setupViewModel(locale: Locale = Locale.US) {
        viewModel = HomeViewModel(
            homeRepository = homeRepository,
            analyticsManager = analyticsManager,
            nextUpdateTimeFormatter = NextUpdateTimeFormatter(
                clock = testClock,
                locale = locale
            ),
            subscriptionInfoDatastore = subscriptionInfoDatastore
        )
    }

    fun setupWeatherRepository(
        weatherUnits: List<WeatherUnit> = fetchedWeatherUnits,
        status: HttpStatusCode = HttpStatusCode.OK,
        geocoderException: Exception? = null,
        lastLocationException: Exception? = null
    ) {
        currentWeatherRepository = CurrentWeatherRepository(
            engine = mockEngine(status, responseValue = getMockedWeather(weatherUnits)),
            locationClient = mockLocationClient(
                geocoderException = geocoderException,
                lastLocationException = lastLocationException
            ),
            currentWeatherDAO = currentWeatherLocalDB.currentWeatherDao(),
            weatherUnitsManager = weatherUnitsManager
        )
    }
    fun setupHomeRepository(
        isSubscribed: IsSubscribed? = null,
        httpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        generatedSuggestions: Suggestions? = null
    ) {
        geminiRepository = mockAIRepository(
            generatedSuggestions = generatedSuggestions,
            httpStatusCode = httpStatusCode
        )
        homeRepository = spyk(
            HomeRepository(
            limitManager = limitManager,
            subscriptionManager = if (isSubscribed != null) mockSubscriptionManager(isSubscribed = isSubscribed) else mockk(),
            currentWeatherRepository = currentWeatherRepository,
            homeAIRepository = geminiRepository,
            dispatcher = testDispatcher
        )
        )
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
        weatherUpdater = spyk(WeatherUpdater(testClock, minutes = 60))
        limitManager = spyk(mockLimitManager(
            limitManagerFirestore = limitManagerFirestore,
            currentWeatherDAO = currentWeatherLocalDB.currentWeatherDao(),
            weatherUpdater = weatherUpdater,
            timeAPI = mockTimeAPI(statusCode = timeApiStatusCode, currTimeMillis = testClock.millis() )
        ))
    }


    private fun mockAIRepository(
        generatedSuggestions: Suggestions? = null,
        httpStatusCode: HttpStatusCode = HttpStatusCode.OK
    ): HomeAIRepository {
        aiClientProvider = mockAIClientProvider(
            statusCode = httpStatusCode,
            responseValue = if (generatedSuggestions != null) Json.encodeToString(generatedSuggestions)
            else ""
        )
        return HomeAIRepository(
            aiClientProvider = aiClientProvider,
            currentWeatherDAO = currentWeatherLocalDB.currentWeatherDao()
        )
    }

    override fun starting(description: Description?) {
        stopKoin()
        unmockkAll()
        analyticsManager = testHelper.getAnalyticsManager(
            AdsDatastoreManager(
                dataStore = createInMemoryDataStore()
            )
        )
        subscriptionInfoDatastore = SubscriptionInfoDatastore(
            dataStore = createInMemoryDataStore()
        )
        currentWeatherLocalDB = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), CurrentWeatherLocalDB::class.java
        ).build()
        setupLimitManager()
        setupWeatherUnitsManager()
        setupWeatherRepository()
        setupHomeRepository(
            isSubscribed = false,
            generatedSuggestions = testSuggestions)
        setupViewModel(locale = Locale.US)
    }

    override fun finished(description: Description?) {
        currentWeatherLocalDB.close()
    }
    val testSuggestions = Suggestions(
        recommendedActivities = listOf("Go for a walk in the park", "Ride a bicycle"),
        unrecommendedActivities = listOf("Avoid swimming outdoors for prolonged periods of time", "Avoid eating ice cream"),
        whatToBring= listOf("Light shoes", "A hat")
    )
    //val generatedSuggestions = testSuggestions.toGeminiResponse()
}

@Serializable
data class TestSuggestions(
    val recommendedActivities: List<String> = listOf("Go for a walk in the park", "Ride a bicycle"),
    val unrecommendedActivities: List<String> = listOf("Avoid swimming outdoors for prolonged periods of time", "Avoid eating ice cream"),
    val whatToBring: List<String> = listOf("Light shoes", "A hat")
)
fun TestSuggestions.toGeminiResponse() =
    GeminiResponse(
        candidates = listOf(GeminiCandidate(
            content = GeminiParts(
                parts = listOf(GeminiPart(
                    text = Json.encodeToString(this)
                ))
            )
        ))
    )

fun getMockedWeather(
    units: List<WeatherUnit>
) = CurrentOpenMeteoWeather(
        latitude = locationInfo.latitude,
        longitude = locationInfo.longitude,
        timezone = "GMT",
        elevation = 34.0,
        currentWeatherUnits = CurrentWeatherUnits(
            time = "iso8601",
            interval = "seconds",
            temperature = units.first { it is WeatherUnit.Temperature }.unitName,
            windSpeed = units.first { it is WeatherUnit.WindSpeed }.unitName,
            windDirection = "°"
        ),
        currentWeather = CurrWeather(
            time = "2023-09-26T12:00:00Z",
            interval = 900,
            temperature = 20.0,
            windSpeed = 15.0,
            windDirection = 270,
            isDay = 1,
            weatherCode = 2
        )
    )

fun TestSuggestions.toSuggestions() = Suggestions(
    recommendedActivities = recommendedActivities,
    unrecommendedActivities = unrecommendedActivities,
    whatToBring = whatToBring
)

fun CurrentOpenMeteoWeather.toCurrentWeather() =
    CurrentWeather(
        locality = "${locationInfo.city}, ${locationInfo.country}",
        tempUnit = this.currentWeatherUnits.temperature,
        windSpeedUnit = this.currentWeatherUnits.windSpeed,
        temp = this.currentWeather.temperature,
        time = this.currentWeather.time,
        windSpeed = this.currentWeather.windSpeed,
        weatherCode = this.currentWeather.weatherCode
    )

