package com.weathersync.features.home.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.utils.CustomResult
import com.weathersync.utils.FirebaseEvent
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import io.mockk.coVerifyAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class HomeCurrentWeatherViewModelTests {
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(homeBaseRule.testDispatcher)

    @After
    fun after() {
        coVerify { homeBaseRule.weatherUnitsManager.getUnits() }
    }

    @Test
    fun getCurrentWeather_limitNotReached_success() = runTest {
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        homeBaseRule.crashlyticsExceptionSlot.apply {
            if (isCaptured) println(captured)
            assertFalse(isCaptured)
        }
        assertEquals(CustomResult.Success, homeBaseRule.viewModel.uiState.value.currentWeatherFetchResult)
        assertEquals(getMockedWeather(fetchedWeatherUnits).toCurrentWeather(), homeBaseRule.viewModel.uiState.value.currentWeather)
        homeBaseRule.homeRepository.apply {
            coVerify { isSubscribed() }
            coVerify { calculateLimit(isSubscribed = false, refresh = false) }
            coVerify { getCurrentWeather(isLimitReached = false) }
            coVerify { generateSuggestions(isLimitReached = false,
                isSubscribed = false,
                currentWeather = getMockedWeather(fetchedWeatherUnits).toCurrentWeather()) }
            coVerify { recordTimestamp() }
            coVerify { insertWeatherAndSuggestions(
                currentWeather = getMockedWeather(fetchedWeatherUnits).toCurrentWeather(),
                suggestions = homeBaseRule.testSuggestions
            ) }
        }
        homeBaseRule.testHelper.apply {
            verifyAnalyticsEvent(FirebaseEvent.CURRENT_WEATHER_FETCH_LIMIT, true)
            verifyAnalyticsEvent(FirebaseEvent.FETCH_CURRENT_WEATHER, false)
            verifyAnalyticsEvent(FirebaseEvent.GENERATE_SUGGESTIONS, false)
        }
    }
    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNull() = runTest {
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis())
        homeBaseRule.apply {
            setupLimitManager(timestamps = timestamps)
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
            viewModel.handleIntent(HomeIntent.GetCurrentWeather)
            advance(this@runTest)
        }
        assertEquals(CustomResult.Success, homeBaseRule.viewModel.uiState.value.currentWeatherFetchResult)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(
            homeBaseRule.viewModel.uiState.value.currentWeather,
            localWeather, homeBaseRule.viewModel.uiState.value.suggestions).all { it == null })
        homeBaseRule.homeRepository.apply {
            coVerify { isSubscribed() }
            coVerify { calculateLimit(isSubscribed = false, refresh = false) }
            coVerify { getCurrentWeather(isLimitReached = true) }
            coVerify(inverse = true) { recordTimestamp() }
            coVerify(inverse = true) { generateSuggestions(isLimitReached = true,
                isSubscribed = false,
                currentWeather = getMockedWeather(fetchedWeatherUnits).toCurrentWeather()) }
        }
        homeBaseRule.testHelper.apply {
            verifyAnalyticsEvent(FirebaseEvent.CURRENT_WEATHER_FETCH_LIMIT, false,
                "next_update_time" to homeBaseRule.viewModel.uiState.value.limit.nextUpdateDateTime!!.toString())
            verifyAnalyticsEvent(FirebaseEvent.FETCH_CURRENT_WEATHER, false)
            verifyAnalyticsEvent(FirebaseEvent.GENERATE_SUGGESTIONS, true)
        }
    }

    @Test
    fun getCurrentWeather_localLimitReached_localWeatherIsNotNull() = runTest {
        homeBaseRule.currentWeatherLocalDB.currentWeatherDao().insertWeather(getMockedWeather(
            fetchedWeatherUnits
        ).toCurrentWeather())

        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        assertEquals(CustomResult.Success, homeBaseRule.viewModel.uiState.value.currentWeatherFetchResult)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(homeBaseRule.viewModel.uiState.value.currentWeather, localWeather).all { it != null })

        homeBaseRule.testHelper.apply {
            verifyAnalyticsEvent(FirebaseEvent.CURRENT_WEATHER_FETCH_LIMIT, false,
                "next_update_time" to "")
            verifyAnalyticsEvent(FirebaseEvent.FETCH_CURRENT_WEATHER, false)
            verifyAnalyticsEvent(FirebaseEvent.GENERATE_SUGGESTIONS, false)
        }
    }
    @Test
    fun fetchCurrentWeather_geocoderError_error() = runTest {
        homeBaseRule.apply {
            setupWeatherRepository(geocoderException = homeBaseRule.exception)
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
        }
        performErrorWeatherFetch(homeBaseRule.exception.message)
    }
    @Test
    fun fetchCurrentWeather_errorResponseStatus_error() = runTest {
        val status = HttpStatusCode.Forbidden
        homeBaseRule.apply {
            setupWeatherRepository(status = status)
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
        }
        performErrorWeatherFetch(status.description)
    }
    @Test
    fun fetchCurrentWeather_lastLocationException_error() = runTest {
        homeBaseRule.apply {
            setupWeatherRepository(lastLocationException = homeBaseRule.exception)
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
        }
        performErrorWeatherFetch(homeBaseRule.exception.message)
    }
    private fun TestScope.performErrorWeatherFetch(message: String?) {
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        val exception = homeBaseRule.crashlyticsExceptionSlot.captured
        exception.apply { if (this is ClientRequestException) assertEquals(message, this.response.status.description)
        else assertEquals(message, this.message)
        }

        assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
        homeBaseRule.homeRepository.apply {
            coVerifyAll {
                isSubscribed()
                calculateLimit(
                    isSubscribed = false,
                    refresh = false)
                getCurrentWeather(isLimitReached = false)
            }
            coVerifyAll(inverse = true) {
                generateSuggestions(isLimitReached = any(),
                    isSubscribed = false,
                    currentWeather = any())
                recordTimestamp()
            }
        }
    }
}