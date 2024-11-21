package com.weathersync.features.home.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
@Entity
data class CurrentWeather(
    @PrimaryKey val uid: Int = 0,
    val locality: String,
    val tempUnit: String,
    val windSpeedUnit: String,
    val temp: Double,
    val windSpeed: Double,
    val time: String? = null,
    val weatherCode: Int,
)

@Serializable
data class CurrentOpenMeteoWeather(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val elevation: Double,
    @SerialName("current_weather_units")
    val currentWeatherUnits: CurrentWeatherUnits,
    @SerialName("current_weather")
    val currentWeather: CurrWeather
)

@Serializable
data class CurrentWeatherUnits(
    val time: String,
    val interval: String,
    val temperature: String,
    @SerialName("windspeed")
    val windSpeed: String,
    @SerialName("winddirection")
    val windDirection: String
)

@Serializable
data class CurrWeather(
    val time: String,
    // in seconds (900 is 15 mins)
    val interval: Int,
    val temperature: Double,
    @SerialName("windspeed")
    val windSpeed: Double,
    @SerialName("winddirection")
    val windDirection: Int,
    @SerialName("is_day")
    val isDay: Int,
    @SerialName("weathercode")
    val weatherCode: Int
)

fun CurrentWeather.isEligibleForUpdate(): Boolean {
    val formatter = DateTimeFormatter.ISO_DATE_TIME
    val currDate = LocalDateTime.now()
    val formattedInputTime = LocalDateTime.parse(time, formatter)
    val duration = Duration.between(formattedInputTime, currDate)
    val isOneHourDifference = duration.toHours() == 1L
    return isOneHourDifference
}