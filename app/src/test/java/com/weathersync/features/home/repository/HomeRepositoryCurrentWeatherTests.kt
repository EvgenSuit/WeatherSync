package com.weathersync.features.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.TestException
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.subscription.IsSubscribed
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRepositoryCurrentWeatherTests {
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
        val weather = getWeather(isSubscribed = false)
        assertTrue(weather != null)
        assertEquals(getMockedWeather(fetchedWeatherUnits).toCurrentWeather(), weather)
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        assertEquals(weather, dao.getWeather())
    }

    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNull() = runTest {
        val fetchedWeather = getWeather(isSubscribed = false, isLimitReached = true)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(fetchedWeather, localWeather).all { it == null })
    }
    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNotNull() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        dao.insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())

        val fetchedWeather = getWeather(isSubscribed = false, isLimitReached = true)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(fetchedWeather, localWeather).all { it != null })
    }
    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNotNullAndOfDifferentWeatherUnits() = runTest {
        // insert weather with units different than the default fetched weather units (km/h vs the default mp/h)
        // so we're converting mp/h to km/h
        val units = listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.WindSpeed.KMH)
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        val insertedWeather = getMockedWeather(units).toCurrentWeather()
        dao.insertWeather(insertedWeather)

        val fetchedWeather = getWeather(isSubscribed = false, isLimitReached = true)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(fetchedWeather, localWeather).all { it != null })

        // assert that the extracted local weather is converted to the up-to-date fetched units
        assertEquals(fetchedWeatherUnits.first { it is WeatherUnit.Temperature }.unitName, fetchedWeather!!.tempUnit)
        assertEquals(fetchedWeatherUnits.first { it is WeatherUnit.WindSpeed }.unitName, fetchedWeather.windSpeedUnit)
        // weather repository rounds the end result to 1 double point
        assertEquals(insertedWeather.windSpeed * 0.621371, fetchedWeather.windSpeed, 0.1)
    }

    @Test(expected = TestException::class)
    fun getCurrentWeather_geocoderError_error() = runTest {
        homeBaseRule.setupWeatherRepository(geocoderException = homeBaseRule.exception)
        getWeather(isSubscribed = false)
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather())
    }

    @Test(expected = ClientRequestException::class)
    fun getCurrentWeather_errorResponseStatus_error() = runTest {
        homeBaseRule.setupWeatherRepository(status = HttpStatusCode.Forbidden)
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather())
        getWeather(isSubscribed = false)
    }
    @Test(expected = TestException::class)
    fun getCurrentWeather_lastLocationException_error() = runTest {
        homeBaseRule.setupWeatherRepository(lastLocationException = homeBaseRule.exception)
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather())
        getWeather(isSubscribed = false)
    }

    private suspend fun getWeather(
        isSubscribed: IsSubscribed,
        isLimitReached: Boolean = false): CurrentWeather? {
        homeBaseRule.setupHomeRepository(isSubscribed = isSubscribed)
        return homeBaseRule.homeRepository.getCurrentWeather(isLimitReached)
    }
}