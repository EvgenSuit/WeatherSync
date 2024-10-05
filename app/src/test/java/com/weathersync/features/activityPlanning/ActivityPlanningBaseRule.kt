package com.weathersync.features.activityPlanning

import com.weathersync.common.utils.locationInfo
import com.weathersync.common.utils.mockCrashlyticsManager
import com.weathersync.common.utils.mockLocationClient
import com.weathersync.features.activityPlanning.data.ForecastUnits
import com.weathersync.features.activityPlanning.data.Hourly
import com.weathersync.features.activityPlanning.data.OpenMeteoForecast
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningIntent
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import com.weathersync.utils.WeatherRepository
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ActivityPlanningBaseRule: TestWatcher() {
    val exceptionSlot = slot<Exception>()
    val exception = Exception("exception")
    lateinit var viewModel: ActivityPlanningViewModel
    fun advance(testScope: TestScope) = repeat(9999999) { testScope.advanceUntilIdle() }
    private var capturedUrl = ""

    override fun starting(description: Description?) {
        stopKoin()
        setup()
        super.starting(description)
    }

    fun setup(
        status: HttpStatusCode = HttpStatusCode.OK,
        geocoderException: Exception? = null,
        lastLocationException: Exception? = null,
        generatedSuggestions: String? = null,
        suggestionsGenerationException: Exception? = null
    ) {
        val weatherRepository = WeatherRepository(
            engine = mockForecastEngine(status),
            locationClient = mockLocationClient(
                geocoderException = geocoderException,
                lastLocationException = lastLocationException
            )
        )
        val activityPlanningRepository = ActivityPlanningRepository(
            weatherRepository = weatherRepository,
            activityPlanningGeminiRepository = mockGeminiRepository(
                generatedContent = generatedSuggestions,
                suggestionsGenerationException = suggestionsGenerationException
            )
        )
        viewModel = ActivityPlanningViewModel(
            activityPlanningRepository = activityPlanningRepository,
            crashlyticsManager = mockCrashlyticsManager(exceptionSlot = exceptionSlot)
        )
    }

    fun mockForecastEngine(
        status: HttpStatusCode
    ): HttpClientEngine = MockEngine { request ->
        capturedUrl = request.url.toString()
        val extractedTimes = extractTimes(capturedUrl)
        val mockedForecast = OpenMeteoForecast(
            forecastUnits = ForecastUnits(
                time = "hours",
                temp = "°C",
                humidity = "%",
                apparentTemp = "°C",
                windSpeed = "km/h",
                precipProb = "%",
                weatherCode = "wmo code",
                visibility = "km",
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
    ) = ActivityPlanningGeminiRepository(
            generativeModel = mockk {
                coEvery { generateContent(any<String>()) } answers {
                    if (suggestionsGenerationException != null) throw suggestionsGenerationException
                    else mockk {
                        every { text } returns (generatedContent ?: generatedSuggestions)
                    }
                }
            }
        )
    val activityPlanningSuggestions = "The best time to do that is October 3, 12:45, 2024"
    private val generatedSuggestions = """
        $activitiesPlanningTag
        $activityPlanningSuggestions
        $activitiesPlanningTag
    """.trimIndent()

    fun assertUrlIsCorrect() {
        assertTrue(capturedUrl.isNotEmpty())
        val extractedTimes = extractTimes(capturedUrl)
        assertDateDifference(extractedTimes.first, extractedTimes.second)
        assertTrue(capturedUrl.contains("latitude=${locationInfo.latitude}&longitude=${locationInfo.longitude}" +
                "&timezone=${ZoneId.systemDefault()}&hourly=temperature_2m,relative_humidity_2m,apparent_temperature," +
                "wind_speed_10m,precipitation_probability,weather_code,visibility,pressure_msl"))
    }
    private fun extractTimes(url: String): Pair<String, String> {
        val startHourRegex = "start_hour=([^&]+)".toRegex()
        val endHourRegex = "end_hour=([^&]+)".toRegex()

        val startHour = startHourRegex.find(url)?.groupValues!![1]
        val endHour = endHourRegex.find(url)?.groupValues!![1]

        return Pair(startHour, endHour)
    }
    private fun assertDateDifference(start: String, end: String) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val startDateTime = LocalDateTime.parse(start, formatter)
        val endDateTime = LocalDateTime.parse(end, formatter)
        val daysBetween = ChronoUnit.DAYS.between(startDateTime, endDateTime)
        assertEquals(5, daysBetween)
    }
}