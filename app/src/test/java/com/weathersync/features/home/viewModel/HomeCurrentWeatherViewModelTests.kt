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
        coVerifyAll {
            homeBaseRule.homeRepository.apply {
                isSubscribed()
                calculateLimit(isSubscribed = false)
                getCurrentWeather(isLimitReached = false)
                generateSuggestions(isLimitReached = false, currentWeather = getMockedWeather(
                    fetchedWeatherUnits
                ).toCurrentWeather())
                recordTimestamp()
            }
        }
        homeBaseRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.CURRENT_WEATHER_FETCH_LIMIT, true)
        homeBaseRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.FETCH_CURRENT_WEATHER, false)
    }
    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNull() = runTest {
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis())
        homeBaseRule.setupLimitManager(timestamps = timestamps)
        homeBaseRule.setupHomeRepository(isSubscribed = false)
        homeBaseRule.setupViewModel()
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(
            homeBaseRule.viewModel.uiState.value.currentWeather,
            localWeather, homeBaseRule.viewModel.uiState.value.suggestions).all { it == null })
        coVerifyAll {
            homeBaseRule.homeRepository.apply {
                isSubscribed()
                calculateLimit(isSubscribed = false)
                getCurrentWeather(isLimitReached = true)
            }
        }
        coVerifyAll(inverse = true) {
            homeBaseRule.homeRepository.apply {
                recordTimestamp()
                generateSuggestions(isLimitReached = true, currentWeather = getMockedWeather(
                    fetchedWeatherUnits
                ).toCurrentWeather())
            }
        }
        homeBaseRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.CURRENT_WEATHER_FETCH_LIMIT, false,
            "next_update_time" to homeBaseRule.viewModel.uiState.value.limit.nextUpdateDateTime!!.toString())
        homeBaseRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.FETCH_CURRENT_WEATHER, false)
    }

    @Test
    fun getCurrentWeather_localLimitReached_localWeatherIsNotNull() = runTest {
        homeBaseRule.currentWeatherLocalDB.currentWeatherDao().insertWeather(getMockedWeather(
            fetchedWeatherUnits
        ).toCurrentWeather())

        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(homeBaseRule.viewModel.uiState.value.currentWeather, localWeather).all { it != null })
        homeBaseRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.CURRENT_WEATHER_FETCH_LIMIT, false,
            "next_update_time" to "")
        homeBaseRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.FETCH_CURRENT_WEATHER, false)
    }
    @Test
    fun fetchCurrentWeather_geocoderError_error() = runTest {
        homeBaseRule.setupWeatherRepository(geocoderException = homeBaseRule.exception)
        homeBaseRule.setupHomeRepository(isSubscribed = false)
        homeBaseRule.setupViewModel()
        performErrorWeatherFetch(homeBaseRule.exception.message)
    }
    @Test
    fun fetchCurrentWeather_errorResponseStatus_error() = runTest {
        val status = HttpStatusCode.Forbidden
        homeBaseRule.setupWeatherRepository(status = status)
        homeBaseRule.setupHomeRepository(isSubscribed = false)
        homeBaseRule.setupViewModel()
        performErrorWeatherFetch(status.description)
    }
    @Test
    fun fetchCurrentWeather_lastLocationException_error() = runTest {
        homeBaseRule.setupWeatherRepository(lastLocationException = homeBaseRule.exception)
        homeBaseRule.setupHomeRepository(isSubscribed = false)
        homeBaseRule.setupViewModel()
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
                calculateLimit(isSubscribed = false)
                getCurrentWeather(isLimitReached = false)
            }
            coVerifyAll(inverse = true) {
                generateSuggestions(isLimitReached = any(), currentWeather = any())
                recordTimestamp()
            }
        }
    }
}