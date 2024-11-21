package com.weathersync.features.home.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.TestException
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.features.home.toSuggestions
import com.weathersync.utils.NullGeminiResponse
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import io.mockk.coVerifyAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class HomeSuggestionsViewModelTests {
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(homeBaseRule.testDispatcher)

    @Test
    fun generateSuggestions_generationException_error() = runTest {
        homeBaseRule.apply {
            setupHomeRepository(
                isSubscribed = false,
                httpStatusCode = HttpStatusCode.Forbidden)
            setupViewModel()
            viewModel.handleIntent(HomeIntent.GetCurrentWeather)
            advance(this@runTest)
        }
        assertTrue(homeBaseRule.viewModel.uiState.value.currentWeather != null)
        assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is ClientRequestException)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                isSubscribed()
                calculateLimit(isSubscribed = false,
                    refresh = false)
                getCurrentWeather(isLimitReached = false)
                generateSuggestions(isLimitReached = false,
                    isSubscribed = false,
                    currentWeather = getMockedWeather(fetchedWeatherUnits).toCurrentWeather())
            }
            coVerifyAll(inverse = true) {
                recordTimestamp()
            }
        }
    }
    @Test
    fun generateSuggestions_emptyGeminiResponse() = runTest {
        homeBaseRule.apply {
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
            viewModel.handleIntent(HomeIntent.GetCurrentWeather)
            advance(this@runTest)
        }
        assertEquals(getMockedWeather(fetchedWeatherUnits).toCurrentWeather(), homeBaseRule.viewModel.uiState.value.currentWeather)
        assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is NullGeminiResponse)
        homeBaseRule.homeRepository.apply {
            coVerify { isSubscribed() }
            coVerify { calculateLimit(isSubscribed = false, refresh = false) }
            coVerify { getCurrentWeather(isLimitReached = false) }
            coVerify { generateSuggestions(
                isLimitReached = false,
                isSubscribed = false,
                currentWeather = getMockedWeather(fetchedWeatherUnits).toCurrentWeather()) }
            coVerify(inverse = true) {
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
            coVerify { isSubscribed() }
            coVerify { calculateLimit(isSubscribed = false, refresh = false) }
            coVerify { getCurrentWeather(isLimitReached = false) }
            coVerify { generateSuggestions(isLimitReached = false,
                isSubscribed = false,
                currentWeather = getMockedWeather(
                    fetchedWeatherUnits
                ).toCurrentWeather()) }
            coVerify { recordTimestamp() }
        }
    }
    @Test
    fun generateSuggestions_USLocale_accountLimitReached_localSuggestionsAreNull() = runTest {
        generateSuggestions_LimitReached(locale = Locale.US)
    }
    @Test
    fun generateSuggestions_UKLocale_accountLimitReached_localSuggestionsAreNull() = runTest {
        generateSuggestions_LimitReached(locale = Locale.UK)
    }
    @Test
    fun generateSuggestions_accountLimitReached_localSuggestionsAreNotNull() = runTest {
        homeBaseRule.currentWeatherLocalDB.currentWeatherDao().apply {
            insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())
            insertSuggestions(homeBaseRule.testSuggestions)
        }
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.apply {
            setupLimitManager(timestamps = timestamps)
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
            viewModel.handleIntent(HomeIntent.GetCurrentWeather)
            advance(this@runTest)
        }
        assertEquals(homeBaseRule.testSuggestions, homeBaseRule.viewModel.uiState.value.suggestions)
        homeBaseRule.homeRepository.apply {
            coVerify { isSubscribed() }
            coVerify { calculateLimit(isSubscribed = false, refresh = false) }
            coVerify { getCurrentWeather(isLimitReached = true) }
            coVerify { generateSuggestions(isLimitReached = true,
                isSubscribed = false,
                currentWeather = getMockedWeather(
                    fetchedWeatherUnits
                ).toCurrentWeather()) }
            coVerify(inverse = true) {
                recordTimestamp()
            }
        }
        assertTrue(homeBaseRule.viewModel.uiState.value.limit.isReached)
    }

    private fun TestScope.generateSuggestions_LimitReached(locale: Locale) {
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.apply {
            setupLimitManager(timestamps = timestamps)
            setupHomeRepository(isSubscribed = false)
            setupViewModel(locale = locale)
            viewModel.handleIntent(HomeIntent.GetCurrentWeather)
            advance(this@generateSuggestions_LimitReached)
        }
        assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                isSubscribed()
                calculateLimit(isSubscribed = false,
                    refresh = false)
                getCurrentWeather(isLimitReached = true)
            }
            coVerify(inverse = true) {
                generateSuggestions(isLimitReached = any(),
                    isSubscribed = false,
                    currentWeather = any())
                recordTimestamp()
            }
        }
        assertTrue(homeBaseRule.viewModel.uiState.value.limit.isReached)
        val nextUpdateDate = homeBaseRule.testHelper.assertNextUpdateTimeIsCorrect(
            receivedNextUpdateDateTime = homeBaseRule.viewModel.uiState.value.limit.nextUpdateDateTime,
            limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
            timestamps = timestamps)
        assertEquals(nextUpdateDate.expectedNextUpdateDate.time, nextUpdateDate.receivedNextUpdateDate.time)
    }
}