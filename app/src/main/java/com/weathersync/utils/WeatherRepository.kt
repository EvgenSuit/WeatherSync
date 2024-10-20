package com.weathersync.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("celsius"),
    FAHRENHEIT("fahrenheit")
}

enum class VisibilityUnit(val symbol: String) {
    METERS("m"),
    KILOMETERS("km"),
    MILES("mi"),
    NAUTICAL_MILES("nm")
}
enum class WindSpeedUnit(val symbol: String) {
    KMH("kmh"),
    MS("ms"),
    MPH("mph"),
    KN("kn")
}

interface WeatherRepository {
    val timezone: ZoneId
        get() = ZoneId.systemDefault()

    fun getHttpClient(engine: HttpClientEngine) =
        HttpClient(engine) {
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
    fun timezoneToUnits(): RequestUnits {
        val country = Locale.getDefault().country
        val tempUnit = when(country) {
            "US", "BS", "LR", "MM" -> TemperatureUnit.FAHRENHEIT
            else -> TemperatureUnit.CELSIUS
        }
        val windSpeedUnit = when (country) {
            "US", "GB" -> WindSpeedUnit.MPH // U.S. and U.K. commonly use mph
            "JP" -> WindSpeedUnit.MS // Japan uses m/s
            "NO" -> WindSpeedUnit.KN // Norway and maritime environments use knots (kn)
            else -> WindSpeedUnit.KMH // Default to km/h for most other countries
        }
        val visibilityUnit = when (country) {
            "US", "GB" -> VisibilityUnit.MILES // U.S. and U.K. use miles
            "JP" -> VisibilityUnit.METERS // Japan uses meters
            "NO" -> VisibilityUnit.NAUTICAL_MILES // Norway uses nautical miles
            else -> VisibilityUnit.KILOMETERS // Default to kilometers
        }
        return RequestUnits(temp = tempUnit, windSpeed = windSpeedUnit, visibility = visibilityUnit)
    }
    fun convertToCorrectDateFormat(time: String): String {
        val formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val formatterOutput = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
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

data class RequestUnits(
    val temp: TemperatureUnit,
    val windSpeed: WindSpeedUnit,
    val visibility: VisibilityUnit
)