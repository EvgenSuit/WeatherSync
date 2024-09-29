package com.weathersync.features.home

import com.weathersync.common.home.mockEngine
import com.weathersync.common.home.mockLocationClient
import com.weathersync.common.mockGeminiRepository
import com.weathersync.common.utils.mockCrashlyticsManager
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.presentation.HomeViewModel
import io.ktor.http.HttpStatusCode
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
            engine = mockEngine(status),
            locationClient = mockLocationClient(
                geocoderException = geocoderException,
                lastLocationException = lastLocationException
            )
        )
        val homeRepository = HomeRepository(
            homeFirebaseClient = mockk(),
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

    override fun starting(description: Description?) {
        stopKoin()
        setup()
    }

}