package com.weathersync.features.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.TestException
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.mockedWeather
import com.weathersync.features.home.toCurrentWeather
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRepositoryCurrentWeatherTests {
    @get: Rule
    val homeBaseRule = HomeBaseRule()

    @Test
    fun getCurrentWeather_limitNotReached_success() = runTest {
        val weather = getWeather()
        assertTrue(weather != null)
        assertEquals(mockedWeather.toCurrentWeather(), weather)
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        assertEquals(weather, dao.getWeather())
    }

    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNull() = runTest {
        val fetchedWeather = getWeather(isLimitReached = true)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(fetchedWeather, localWeather).all { it == null })
    }
    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNotNull() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        dao.insertWeather(mockedWeather.toCurrentWeather())

        val fetchedWeather = getWeather(isLimitReached = true)
        val localWeather = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather()
        assertTrue(listOf(fetchedWeather, localWeather).all { it != null })
    }

    @Test(expected = TestException::class)
    fun getCurrentWeather_geocoderError_error() = runTest {
        homeBaseRule.setupWeatherRepository(geocoderException = homeBaseRule.exception)
        getWeather()
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather())
    }

    @Test(expected = ClientRequestException::class)
    fun getCurrentWeather_errorResponseStatus_error() = runTest {
        homeBaseRule.setupWeatherRepository(status = HttpStatusCode.Forbidden)
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather())
        getWeather()
    }
    @Test(expected = TestException::class)
    fun getCurrentWeather_lastLocationException_error() = runTest {
        homeBaseRule.setupWeatherRepository(lastLocationException = homeBaseRule.exception)
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getWeather())
        getWeather()
    }

    private suspend fun getWeather(isLimitReached: Boolean = false): CurrentWeather? {
        homeBaseRule.setupHomeRepository()
        return homeBaseRule.homeRepository.getCurrentWeather(isLimitReached)
    }
}