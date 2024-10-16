package com.weathersync.features.home

import android.Manifest
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestClock
import com.weathersync.common.TestException
import com.weathersync.common.TestHelper
import com.weathersync.common.mockEngine
import com.weathersync.common.mockGenerativeModel
import com.weathersync.common.utils.locationInfo
import com.weathersync.common.utils.mockLimitManager
import com.weathersync.common.utils.mockLimitManagerFirestore
import com.weathersync.common.utils.mockLocationClient
import com.weathersync.features.home.data.CurrWeather
import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.CurrentWeatherUnits
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.db.CurrentWeatherLocalDB
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.presentation.HomeViewModel
import com.weathersync.utils.LimitManager
import com.weathersync.utils.LimitManagerConfig
import io.ktor.http.HttpStatusCode
import io.mockk.spyk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.stopKoin
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

class HomeBaseRule: TestWatcher() {
    val limitManagerConfig = LimitManagerConfig(2, 24)
    val testHelper = TestHelper()
    val testClock = TestClock()

    val crashlyticsExceptionSlot = testHelper.crashlyticsExceptionSlot
    val exception = TestException("exception")
    val snackbarScope = TestScope()
    val crashlyticsManager = testHelper.crashlyticsManager
    lateinit var viewModel: HomeViewModel
    fun advance(testScope: TestScope) = repeat(99999999) { testScope.advanceUntilIdle() }

    lateinit var weatherUpdater: WeatherUpdater
    lateinit var currentWeatherRepository: CurrentWeatherRepository
    lateinit var limitManager: LimitManager
    lateinit var currentWeatherLocalDB: CurrentWeatherLocalDB
    lateinit var homeRepository: HomeRepository
    lateinit var limitManagerFirestore: FirebaseFirestore
    lateinit var generativeModel: GenerativeModel
    lateinit var geminiRepository: GeminiRepository

    fun manageLocationPermission(grant: Boolean) {
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        val shadowApp = Shadows.shadowOf(RuntimeEnvironment.getApplication())
        shadowApp.apply { if (grant) grantPermissions(permission) else denyPermissions(permission) }
    }

    fun setupViewModel() {
        viewModel = HomeViewModel(
            homeRepository = homeRepository,
            crashlyticsManager = crashlyticsManager
        )
    }

    fun setupWeatherRepository(
        status: HttpStatusCode = HttpStatusCode.OK,
        geocoderException: Exception? = null,
        lastLocationException: Exception? = null
    ) {
        currentWeatherRepository = CurrentWeatherRepository(
            engine = mockEngine(status, responseValue = mockedWeather),
            locationClient = mockLocationClient(
                geocoderException = geocoderException,
                lastLocationException = lastLocationException
            ),
            currentWeatherDAO = currentWeatherLocalDB.currentWeatherDao()
        )
    }
    fun setupHomeRepository(
        generatedSuggestions: String? = null,
        suggestionsGenerationException: Exception? = null
    ) {
        geminiRepository = mockGeminiRepository(
            generatedContent = generatedSuggestions,
            suggestionsGenerationException = suggestionsGenerationException
        )
        homeRepository = spyk(HomeRepository(
            limitManager = limitManager,
            currentWeatherRepository = currentWeatherRepository,
            geminiRepository = geminiRepository
        ))
    }
    fun setupLimitManager(
        timestamps: List<Timestamp> = listOf(),
        limitManagerConfig: LimitManagerConfig,
        serverTimestampGetException: Exception? = null,
        serverTimestampDeleteException: Exception? = null) {
        limitManagerFirestore = mockLimitManagerFirestore(
            testClock = testClock,
            limitManagerConfig = limitManagerConfig,
            timestamps = timestamps,
            serverTimestampGetException = serverTimestampGetException,
            serverTimestampDeleteException = serverTimestampDeleteException
        )
        weatherUpdater = spyk(WeatherUpdater(testClock, minutes = 60))
        limitManager = mockLimitManager(
            limitManagerConfig = limitManagerConfig,
            limitManagerFirestore = limitManagerFirestore,
            currentWeatherDAO = currentWeatherLocalDB.currentWeatherDao(),
            weatherUpdater = weatherUpdater
        )
    }


    private fun mockGeminiRepository(
        generatedContent: String? = null,
        suggestionsGenerationException: Exception? = null
    ): GeminiRepository {
        generativeModel = mockGenerativeModel(
            generatedContent = generatedContent,
            generationException = suggestionsGenerationException
        )
        return GeminiRepository(
            generativeModel = generativeModel,
            currentWeatherDAO = currentWeatherLocalDB.currentWeatherDao()
        )
    }

    override fun starting(description: Description?) {
        stopKoin()
        currentWeatherLocalDB = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), CurrentWeatherLocalDB::class.java
        ).build()
        setupLimitManager(
            limitManagerConfig = LimitManagerConfig(6, 6)
        )
        setupWeatherRepository()
        setupHomeRepository(generatedSuggestions = generatedSuggestions)
        setupViewModel()
    }

    override fun finished(description: Description?) {
        currentWeatherLocalDB.close()
    }
    val testSuggestions = TestSuggestions()
    val generatedSuggestions = """
    $recommendedActivitiesTag
    ${testSuggestions.recommendedActivities[0]}
    ${testSuggestions.recommendedActivities[1]}
    $recommendedActivitiesTag
    $unrecommendedActivitiesTag
    ${testSuggestions.unrecommendedActivities[0]}
    ${testSuggestions.unrecommendedActivities[1]}
    $unrecommendedActivitiesTag
    $whatToBringTag     
    ${testSuggestions.whatToBring[0]}
    ${testSuggestions.whatToBring[1]}   
    $whatToBringTag
""".trimIndent()
}

data class TestSuggestions(
    val recommendedActivities: List<String> = listOf("Go for a walk in the park", "Ride a bicycle"),
    val unrecommendedActivities: List<String> = listOf("Avoid swimming outdoors for prolonged periods of time", "Avoid eating ice cream"),
    val whatToBring: List<String> = listOf("Light shoes", "A hat")
)
val mockedWeather = CurrentOpenMeteoWeather(
    latitude = locationInfo.latitude,
    longitude = locationInfo.longitude,
    timezone = "GMT",
    elevation = 34.0,
    currentWeatherUnits = CurrentWeatherUnits(
        time = "iso8601",
        interval = "seconds",
        temperature = "°C",
        windSpeed = "km/h",
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

