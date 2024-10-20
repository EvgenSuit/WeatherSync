package com.weathersync.features.home

import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import com.weathersync.utils.WeatherRepository
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking

class CurrentWeatherRepository(
    engine: HttpClientEngine,
    private val locationClient: LocationClient,
    private val currentWeatherDAO: CurrentWeatherDAO
    ): WeatherRepository {
    private val httpClient = getHttpClient(engine)

    suspend fun getCurrentWeather(isLimitReached: Boolean): CurrentWeather? {
        if (isLimitReached) return currentWeatherDAO.getWeather()
        val coordinates = locationClient.getCoordinates()
        val requestUnits = timezoneToUnits()
        val requestUrl =
            "forecast?current_weather=true&latitude=${coordinates.lat}&longitude=${coordinates.lon}&timezone=$timezone" +
                    "&temperature_unit=${requestUnits.temp.symbol}&wind_speed_unit=${requestUnits.windSpeed.symbol}"
        val responseBody = httpClient.get(requestUrl).body<CurrentOpenMeteoWeather>()
        val currentWeather = CurrentWeather(
            locality = coordinates.locality,
            tempUnit = responseBody.currentWeatherUnits.temperature,
            windSpeedUnit = responseBody.currentWeatherUnits.windSpeed,
            temp = responseBody.currentWeather.temperature,
            windSpeed = responseBody.currentWeather.windSpeed,
            time = responseBody.currentWeather.time,
            weatherCode = responseBody.currentWeather.weatherCode
        )
        currentWeatherDAO.insertWeather(currentWeather)
        return currentWeather
    }
}
