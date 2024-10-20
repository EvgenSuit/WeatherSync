package com.weathersync.features.home.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.weathersync.common.TestException
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.mockedWeather
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.features.home.toSuggestions
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import com.weathersync.utils.EmptyGeminiResponse
import com.weathersync.utils.LimitManagerConfig
import io.mockk.coVerify
import io.mockk.coVerifyAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeSuggestionsViewModelTests {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()

    @Test
    fun generateSuggestions_generationException_error() = runTest {
        homeBaseRule.setupHomeRepository(suggestionsGenerationException = homeBaseRule.exception)
        homeBaseRule.setupViewModel()
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        assertTrue(homeBaseRule.viewModel.uiState.value.currentWeather != null)
        assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is TestException)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                calculateLimit()
                getCurrentWeather(isLimitReached = false)
                generateSuggestions(isLimitReached = false, currentWeather = mockedWeather.toCurrentWeather())
            }
            coVerifyAll(inverse = true) {
                recordTimestamp()
            }
        }
    }
    @Test
    fun generateSuggestions_emptyGeminiResponse() = runTest {
        homeBaseRule.setupHomeRepository(generatedSuggestions = null)
        homeBaseRule.setupViewModel()
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        assertEquals(mockedWeather.toCurrentWeather(), homeBaseRule.viewModel.uiState.value.currentWeather)
        assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is EmptyGeminiResponse)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                calculateLimit()
                getCurrentWeather(isLimitReached = false)
                generateSuggestions(
                    isLimitReached = false,
                    currentWeather = mockedWeather.toCurrentWeather()
                )
            }
            coVerifyAll(inverse = true) {
                recordTimestamp()
            }
        }
    }
    @Test
    fun generateSuggestions_atLeastOneTagMissing() = runTest {
        homeBaseRule.setupHomeRepository(generatedSuggestions = "Content with no tags")
        homeBaseRule.setupViewModel()
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        assertEquals(mockedWeather.toCurrentWeather(), homeBaseRule.viewModel.uiState.value.currentWeather)
        assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is AtLeastOneGenerationTagMissing)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                calculateLimit()
                getCurrentWeather(isLimitReached = false)
                generateSuggestions(
                    isLimitReached = false,
                    currentWeather = mockedWeather.toCurrentWeather()
                )
            }
            coVerifyAll(inverse = true) {
                recordTimestamp()
            }
        }
    }
    @Test
    fun generateSuggestions_success() = runTest {
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        assertTrue(homeBaseRule.viewModel.uiState.value.currentWeather != null)
        assertTrue(!homeBaseRule.crashlyticsExceptionSlot.isCaptured)
        val suggestions = homeBaseRule.viewModel.uiState.value.suggestions
        assertEquals(homeBaseRule.testSuggestions.recommendedActivities, suggestions!!.recommendedActivities)
        assertEquals(homeBaseRule.testSuggestions.unrecommendedActivities, suggestions.unrecommendedActivities)
        assertEquals(homeBaseRule.testSuggestions.whatToBring, suggestions.whatToBring)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                calculateLimit()
                getCurrentWeather(isLimitReached = false)
                generateSuggestions(isLimitReached = false, currentWeather = mockedWeather.toCurrentWeather())
                recordTimestamp()
            }
        }
    }
    @Test
    fun generateSuggestions_accountLimitReached_localSuggestionsAreNull() = runTest {
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.setupLimitManager(
            timestamps = timestamps,
            limitManagerConfig = homeBaseRule.limitManagerConfig
        )
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                calculateLimit()
                getCurrentWeather(isLimitReached = true)
            }
            coVerify(inverse = true) {
                generateSuggestions(isLimitReached = any(), currentWeather = any())
                recordTimestamp()
            }
        }
        assertTrue(homeBaseRule.viewModel.uiState.value.limit.isReached)
    }
    @Test
    fun generateSuggestions_accountLimitReached_localSuggestionsAreNotNull() = runTest {
        homeBaseRule.currentWeatherLocalDB.currentWeatherDao().apply {
            insertWeather(mockedWeather.toCurrentWeather())
            insertSuggestions(homeBaseRule.testSuggestions.toSuggestions())
        }
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.setupLimitManager(
            timestamps = timestamps,
            limitManagerConfig = homeBaseRule.limitManagerConfig
        )
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        assertEquals(homeBaseRule.testSuggestions.toSuggestions(), homeBaseRule.viewModel.uiState.value.suggestions)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                calculateLimit()
                getCurrentWeather(isLimitReached = true)
                generateSuggestions(isLimitReached = true, currentWeather = mockedWeather.toCurrentWeather())
            }
            coVerify(inverse = true) {
                recordTimestamp()
            }
        }
        assertTrue(homeBaseRule.viewModel.uiState.value.limit.isReached)
    }

}