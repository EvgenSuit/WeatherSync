package com.weathersync.features.home

import com.weathersync.features.home.data.Coordinates
import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.Forecast
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class WeatherRepository(
    engine: HttpClientEngine,
    private val locationClient: LocationClient
    ) {
    private val httpClient = HttpClient(engine) {
        install(Logging)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        defaultRequest {
            url("https://api.open-meteo.com/v1/")
        }
    }
    suspend fun getCurrentWeather(): CurrentWeather {
        val coordinates = locationClient.getCoordinates()
        val requestUrl = "forecast?current_weather=true&latitude=${coordinates.lat}&longitude=${coordinates.lon}"
        val response = httpClient.get(requestUrl)
        if (response.status.isSuccess()) {
            val responseBody = response.body<CurrentOpenMeteoWeather>()
            return CurrentWeather(
                locality = coordinates.locality,
                tempUnit = responseBody.currentWeatherUnits.temperature,
                windSpeedUnit = responseBody.currentWeatherUnits.windSpeed,
                temp = responseBody.currentWeather.temperature,
                windSpeed = responseBody.currentWeather.windSpeed,
                weatherCode = responseBody.currentWeather.weatherCode
            )
        } else throw Exception(response.status.description)
    }
    suspend fun getForecast(forecastDays: Int,
                            currentWeather: Boolean,
                            coordinates: Coordinates): Forecast {
        val requestUrl = "forecast?forecast_days=$forecastDays&latitude=${coordinates.lat}&longitude=${coordinates.lon}&hourly=temperature_2m,relativehumidity_2m,windspeed_10m,pressure_msl"
        return httpClient.get(requestUrl).body()
    }
}