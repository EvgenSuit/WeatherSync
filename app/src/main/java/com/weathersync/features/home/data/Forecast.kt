package com.weathersync.features.home.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Forecast(
    val latitude: Float,
    val longitude: Float,
    val generationtime_ms: Float,
    val utc_offset_seconds: Int,
    val timezone: String,
    @SerialName("timezone_abbreviation")
    val timezoneAbbreviation: String,
    val elevation: Float,
    @SerialName("hourly_units")
    val hourlyUnits: HourlyUnits,
    val hourly: Hourly
)

@Serializable
data class Hourly(
    val time: List<String>,
    @SerialName("temperature_2m")
    val temperature: List<Float>,
    @SerialName("relativehumidity_2m")
    val humidity: List<Int>,
    @SerialName("windspeed_10m")
    val windSpeed: List<Float>,
    @SerialName("pressure_msl")
    val pressure: List<Float>
)

@Serializable
data class HourlyUnits(
    val time: String,
    @SerialName("temperature_2m")
    val temperature: String,
    @SerialName("relativehumidity_2m")
    val humidity: String,
    @SerialName("windspeed_10m")
    val windSpeed: String,
    @SerialName("pressure_msl")
    val pressure: String
)
