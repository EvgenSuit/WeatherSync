package com.weathersync.utils

import com.weathersync.features.activityPlanning.data.OpenMeteoForecast
import com.weathersync.features.activityPlanning.data.ForecastDates
import com.weathersync.features.home.LocationClient
import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeather
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherRepository(
    engine: HttpClientEngine,
    private val locationClient: LocationClient
    ) {
    private val timezone = ZoneId.systemDefault()
    private val httpClient = HttpClient(engine) {
        expectSuccess = true
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
        val requestUrl =
            "forecast?current_weather=true&latitude=${coordinates.lat}&longitude=${coordinates.lon}&timezone=$timezone"
        val responseBody = httpClient.get(requestUrl).body<CurrentOpenMeteoWeather>()
        return CurrentWeather(
            locality = coordinates.locality,
            tempUnit = responseBody.currentWeatherUnits.temperature,
            windSpeedUnit = responseBody.currentWeatherUnits.windSpeed,
            temp = responseBody.currentWeather.temperature,
            windSpeed = responseBody.currentWeather.windSpeed,
            time = responseBody.currentWeather.time,
            weatherCode = responseBody.currentWeather.weatherCode
        )
    }

    // make this request to see what the forecast looks like in plain json:
    // https://api.open-meteo.com/v1/forecast?latitude=51.1&longitude=17.03333&start_hour=2024-09-30T23:00&end_hour=2024-10-01T23:00&timezone=Europe/Warsaw&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,precipitation_probability,weather_code,visibility,pressure_msl
    suspend fun getForecast(forecastDates: ForecastDates): OpenMeteoForecast {
        val coordinates = locationClient.getCoordinates()
        val requestUrl =
            "forecast?start_hour=${forecastDates.startDate}&end_hour=${forecastDates.endDate}" +
                    "&latitude=${coordinates.lat}&longitude=${coordinates.lon}" +
                    "&timezone=$timezone&hourly=temperature_2m,relative_humidity_2m,apparent_temperature," +
                    "wind_speed_10m,precipitation_probability,weather_code,visibility,pressure_msl"
        val forecast = httpClient.get(requestUrl).body<OpenMeteoForecast>()
        return forecast.copy(
            hourly = forecast.hourly.copy(
                time = forecast.hourly.time.map(::convertToCorrectDateFormat)
            ),
            locality = coordinates.locality
        )
    }
    private fun convertToCorrectDateFormat(time: String): String {
        val formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val formatterOutput = DateTimeFormatter.ofPattern("MMMM d, yyyy, HH:mm", Locale.getDefault())
        val dateTime = LocalDateTime.parse(time, formatterInput)
        return dateTime.format(formatterOutput)
    }
}

fun weatherCodeToDescription(weatherCode: Int): String =
    when (weatherCode) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45 -> "Fog"
        48 -> "Depositing rime fog"
        51 -> "Light drizzle"
        53 -> "Moderate drizzle"
        55 -> "Dense intensity drizzle"
        56 -> "Light freezing drizzle"
        57 -> "Dense freezing drizzle"
        61 -> "Slight rain"
        63 -> "Moderate rain"
        65 -> "Heavy rain"
        66 -> "Light freezing rain"
        67 -> "Heavy freezing rain"
        71 -> "Slight snowfall"
        73 -> "Moderate snowfall"
        75 -> "Heavy snowfall"
        77 -> "Snow grains"
        80 -> "Slight rain showers"
        81 -> "Moderate rain showers"
        82 -> "Violent rain showers"
        85 -> "Slight snow showers"
        86 -> "Heavy snow showers"
        95 -> "Slight or moderate thunderstorm"
        96 -> "Thunderstorm with slight hail"
        99 -> "Thunderstorm with heavy hail"
        else -> "Unknown weather code"
    }