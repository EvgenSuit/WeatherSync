package com.weathersync.features.home.unit

import com.weathersync.common.home.mockedWeather
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.common.utils.locationInfo
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.presentation.HomeIntent
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class HomeCurrentWeatherUnitTests {
    // set order to 0 since HomeBaseRule's starting method immediately executes after homeRule is created,
    // which makes the view model initialize with the default Main dispatcher
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val homeRule = HomeBaseRule()

    @Test
    fun fetchCurrentWeather_success() = runTest {
        homeRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeRule.advanceKtor(this)
        assertFalse(homeRule.crashlyticsExceptionSlot.isCaptured)
        assertEquals(CurrentWeather(
            locality = "${locationInfo.city}, ${locationInfo.country}",
            tempUnit = mockedWeather.currentWeatherUnits.temperature,
            windSpeedUnit = mockedWeather.currentWeatherUnits.windSpeed,
            temp = mockedWeather.currentWeather.temperature,
            time = mockedWeather.currentWeather.time,
            windSpeed = mockedWeather.currentWeather.windSpeed,
            weatherCode = mockedWeather.currentWeather.weatherCode
        ), homeRule.viewModel.uiState.value.currentWeather)
    }
    @Test
    fun fetchCurrentWeather_geocoderError_error() = runTest {
        homeRule.setup(geocoderException = homeRule.exception)
        performErrorWeatherFetch(homeRule.exception.message)
    }
    @Test
    fun fetchCurrentWeather_errorResponseStatus_error() = runTest {
        val status = HttpStatusCode.Forbidden
        homeRule.setup(status)
        performErrorWeatherFetch(status.description)
    }
    @Test
    fun fetchCurrentWeather_lastLocationException_error() = runTest {
        homeRule.setup(lastLocationException = homeRule.exception)
        performErrorWeatherFetch(homeRule.exception.message)
    }
    private fun TestScope.performErrorWeatherFetch(message: String?) {
        homeRule.viewModel.handleIntent(HomeIntent.GetCurrentWeather)
        homeRule.advanceKtor(this)
        val exception = homeRule.crashlyticsExceptionSlot.captured
        exception.apply { if (this is ClientRequestException) assertEquals(message, this.response.status.description)
        else assertEquals(message, this.message)
        }

        assertEquals(null, homeRule.viewModel.uiState.value.currentWeather)
    }
}