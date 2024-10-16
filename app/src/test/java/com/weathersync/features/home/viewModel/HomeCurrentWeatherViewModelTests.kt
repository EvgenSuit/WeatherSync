package com.weathersync.features.home.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.mockedWeather
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.utils.LimitManagerConfig
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import io.mockk.coVerifyAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class HomeCurrentWeatherViewModelTests {
    // set order to 0 since HomeBaseRule's starting method immediately executes after homeRule is created,
    // which makes the view model initialize with the default Main dispatcher if it's not set to a different one
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()

    @Test
    fun getCurrentWeather_limitNotReached_success() = runTest {
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        homeBaseRule.crashlyticsExceptionSlot.apply {
            if (isCaptured) println(captured)
            assertFalse(isCaptured)
        }
        assertEquals(mockedWeather.toCurrentWeather(), homeBaseRule.viewModel.uiState.value.currentWeather)
        coVerifyAll {
            homeBaseRule.homeRepository.apply {
                calculateLimit()
                getCurrentWeather(isLimitReached = false)
                generateSuggestions(isLimitReached = false, currentWeather = mockedWeather.toCurrentWeather())
                recordTimestamp()
            }
        }
    }
    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNull() = runTest {
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis())
        homeBaseRule.setupLimitManager(timestamps = timestamps, limitManagerConfig = homeBaseRule.limitManagerConfig)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(
            homeBaseRule.viewModel.uiState.value.currentWeather,
            localWeather, homeBaseRule.viewModel.uiState.value.suggestions).all { it == null })
        coVerifyAll {
            homeBaseRule.homeRepository.apply {
                calculateLimit()
                getCurrentWeather(isLimitReached = true)
            }
        }
        coVerifyAll(inverse = true) {
            homeBaseRule.homeRepository.apply {
                recordTimestamp()
                generateSuggestions(isLimitReached = true, currentWeather = mockedWeather.toCurrentWeather())
            }
        }
    }
    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNotNull() = runTest {
        homeBaseRule.currentWeatherLocalDB.currentWeatherDao().insertWeather(mockedWeather.toCurrentWeather())

        homeBaseRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeBaseRule.advance(this)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(homeBaseRule.viewModel.uiState.value.currentWeather, localWeather).all { it != null })
    }
    @Test
    fun fetchCurrentWeather_geocoderError_error() = runTest {
        homeBaseRule.setupWeatherRepository(geocoderException = homeBaseRule.exception)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        performErrorWeatherFetch(homeBaseRule.exception.message)

    }
    @Test
    fun fetchCurrentWeather_errorResponseStatus_error() = runTest {
        val status = HttpStatusCode.Forbidden
        homeBaseRule.setupWeatherRepository(status = status)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        performErrorWeatherFetch(status.description)
    }
    @Test
    fun fetchCurrentWeather_lastLocationException_error() = runTest {
        homeBaseRule.setupWeatherRepository(lastLocationException = homeBaseRule.exception)
        homeBaseRule.setupHomeRepository()
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
                calculateLimit()
                getCurrentWeather(isLimitReached = false)
            }
            coVerifyAll(inverse = true) {
                generateSuggestions(isLimitReached = any(), currentWeather = any())
                recordTimestamp()
            }
        }
    }
}