package com.weathersync.features.home

import com.weathersync.common.home.mockCurrentWeatherEngine
import com.weathersync.common.utils.mockCrashlyticsManager
import com.weathersync.common.utils.mockLocationClient
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.presentation.HomeViewModel
import com.weathersync.utils.WeatherRepository
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.stopKoin

class HomeBaseRule: TestWatcher() {

    val crashlyticsExceptionSlot = slot<Exception>()
    val exception = Exception("exception")
    val snackbarScope = TestScope()
    val crashlyticsManager = mockCrashlyticsManager(exceptionSlot = crashlyticsExceptionSlot)
    lateinit var viewModel: HomeViewModel
    fun advanceKtor(testScope: TestScope) = repeat(9999999) { testScope.advanceUntilIdle() }

    fun setup(
        status: HttpStatusCode = HttpStatusCode.OK,
        geocoderException: Exception? = null,
        lastLocationException: Exception? = null,
        generatedSuggestions: String? = null,
        suggestionsGenerationException: Exception? = null
    ) {
        val weatherRepository = WeatherRepository(
            engine = mockCurrentWeatherEngine(status),
            locationClient = mockLocationClient(
                geocoderException = geocoderException,
                lastLocationException = lastLocationException
            )
        )
        val homeRepository = HomeRepository(
            limitManager = mockk(),
            weatherRepository = weatherRepository,
            geminiRepository = mockGeminiRepository(
                generatedContent = generatedSuggestions,
                suggestionsGenerationException = suggestionsGenerationException
            )
        )
        viewModel = HomeViewModel(
            homeRepository = homeRepository,
            crashlyticsManager = crashlyticsManager
        )
    }

    fun getCurrentWeather(testScope: TestScope, success: Boolean) {
        viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        advanceKtor(testScope)
        (viewModel.uiState.value.currentWeather != null).apply { if (success) assertTrue(this) else assertFalse(this) }
    }

    private fun mockGeminiRepository(
        generatedContent: String? = null,
        suggestionsGenerationException: Exception? = null
    ): GeminiRepository =
        GeminiRepository(
            generativeModel = mockk {
                coEvery { generateContent(any<String>()) } answers {
                    if (suggestionsGenerationException != null) throw suggestionsGenerationException
                    else mockk {
                        every { text } returns (generatedContent ?: generatedSuggestions)
                    }
                }
            }
        )
    override fun starting(description: Description?) {
        stopKoin()
        setup()
    }
    data class TestSuggestions(
        val recommendedActivities: List<String> = listOf("Go for a walk in the park", "Ride a bicycle"),
        val unrecommendedActivities: List<String> = listOf("Avoid swimming outdoors for prolonged periods of time", "Avoid eating ice cream"),
        val whatToBring: List<String> = listOf("Light shoes", "A hat")
    )
    val testSuggestions = TestSuggestions()
    private val generatedSuggestions = """
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
