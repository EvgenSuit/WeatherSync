package com.weathersync.features.home.unit

import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.utils.AtLeastOneTagMissing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRecommendationsUnitTests {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()

    @Test
    fun generateSuggestions_generationException_error() = runTest {
        homeBaseRule.setup(suggestionsGenerationException = homeBaseRule.exception)
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advanceKtor(this)
        assertTrue(homeBaseRule.viewModel.uiState.value.currentWeather != null)
        advanceUntilIdle()
        assertEquals(homeBaseRule.exception.message, homeBaseRule.crashlyticsExceptionSlot.captured.message)
    }
    @Test
    fun generateSuggestions_atLeastOneTagMissing_error() = runTest {
        homeBaseRule.setup(generatedSuggestions = "Content without tags")
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advanceKtor(this)
        assertTrue(homeBaseRule.viewModel.uiState.value.currentWeather != null)
        advanceUntilIdle()
        assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is AtLeastOneTagMissing)
    }
    @Test
    fun generateSuggestions_success() = runTest {
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advanceKtor(this)
        assertTrue(homeBaseRule.viewModel.uiState.value.currentWeather != null)
        advanceUntilIdle()
        assertTrue(!homeBaseRule.crashlyticsExceptionSlot.isCaptured)
        val suggestions = homeBaseRule.viewModel.uiState.value.suggestions
        assertEquals(homeBaseRule.testSuggestions.recommendedActivities, suggestions.recommendedActivities)
        assertEquals(homeBaseRule.testSuggestions.unrecommendedActivities, suggestions.unrecommendedActivities)
        assertEquals(homeBaseRule.testSuggestions.whatToBring, suggestions.whatToBring)
    }
}