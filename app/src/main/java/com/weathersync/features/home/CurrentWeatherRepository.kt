package com.weathersync.features.home

import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.weather.LocationClient
import com.weathersync.utils.weather.WeatherUnitsManager
import com.weathersync.utils.weather.WeatherRepository
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get

class CurrentWeatherRepository(
    engine: HttpClientEngine,
    private val locationClient: LocationClient,
    private val currentWeatherDAO: CurrentWeatherDAO,
    private val weatherUnitsManager: WeatherUnitsManager
    ): WeatherRepository {
    private val httpClient = getHttpClient(engine)

    suspend fun getCurrentWeather(isLimitReached: Boolean): CurrentWeather? {
        val fetchedUnits = weatherUnitsManager.getUnits()
        if (isLimitReached) {
            val localWeather = currentWeatherDAO.getWeather() ?: return null
            // convert local weather units to the right units
            return localWeather.copy(
                tempUnit = fetchedUnits.temp.unitName,
                windSpeedUnit = fetchedUnits.windSpeed.unitName,
                temp = convertTemperature(localWeather.temp, from = localWeather.tempUnit, to = fetchedUnits.temp.unitName).round(1),
                windSpeed = convertWindSpeed(localWeather.windSpeed, from = localWeather.windSpeedUnit, to = fetchedUnits.windSpeed.unitName).round(1)
            )
        }
        val coordinates = locationClient.getCoordinates()
        val requestUrl =
            "forecast?current_weather=true&latitude=${coordinates.lat}&longitude=${coordinates.lon}&timezone=$timezone" +
                    "&temperature_unit=${fetchedUnits.temp.weatherApiUnitName}&wind_speed_unit=${fetchedUnits.windSpeed.weatherApiUnitName}"
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
    private fun convertTemperature(temp: Double, from: String, to: String): Double {
        return when (from to to) {
            WeatherUnit.Temperature.Celsius.unitName to WeatherUnit.Temperature.Fahrenheit.unitName -> (temp * 9 / 5) + 32
            WeatherUnit.Temperature.Fahrenheit.unitName to WeatherUnit.Temperature.Celsius.unitName -> (temp - 32) * 5 / 9
            else -> temp // No conversion needed if from and to units are the same
        }
    }

    private fun convertWindSpeed(speed: Double, from: String, to: String): Double {
        return when (from to to) {
            WeatherUnit.WindSpeed.KMH.unitName to WeatherUnit.WindSpeed.MS.unitName -> speed / 3.6
            WeatherUnit.WindSpeed.MS.unitName to WeatherUnit.WindSpeed.KMH.unitName -> speed * 3.6
            WeatherUnit.WindSpeed.KMH.unitName to WeatherUnit.WindSpeed.MPH.unitName -> speed * 0.621371
            WeatherUnit.WindSpeed.MPH.unitName to WeatherUnit.WindSpeed.KMH.unitName -> speed / 0.621371
            WeatherUnit.WindSpeed.MS.unitName to WeatherUnit.WindSpeed.MPH.unitName -> speed * 2.23694
            WeatherUnit.WindSpeed.MPH.unitName to WeatherUnit.WindSpeed.MS.unitName -> speed / 2.23694
            else -> speed // No conversion needed if from and to units are the same
        }
    }

}
