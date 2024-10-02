package com.weathersync.features.activityPlanning.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Forecast(
    val forecast: List<SingleForecast>
)
data class SingleForecast(
    val time: ForecastValue,
    val temp: ForecastValue,
    val humidity: ForecastValue,
    val apparentTemp: ForecastValue,
    val windSpeed: ForecastValue,
    val precipProb: ForecastValue,
    val weatherCode: ForecastValue,
    val visibility: ForecastValue,
    val pressure: ForecastValue
)
data class ForecastValue(
    val value: Any,
    val unit: String,
)

@Serializable
data class OpenMeteoForecast(
    @SerialName("hourly_units")
    val forecastUnits: ForecastUnits,
    val hourly: Hourly
)

@Serializable
data class ForecastUnits(
    val time: String,
    @SerialName("temperature_2m")
    val temp: String,
    @SerialName("relative_humidity_2m")
    val humidity: String,
    @SerialName("apparent_temperature")
    val apparentTemp: String,
    @SerialName("wind_speed_10m")
    val windSpeed: String,
    @SerialName("precipitation_probability")
    val precipProb: String,
    @SerialName("weather_code")
    val weatherCode: String,
    @SerialName("visibility")
    val visibility: String,
    @SerialName("pressure_msl")
    val pressure: String
)
@Serializable
data class Hourly(
    val time: List<String>,
    @SerialName("temperature_2m")
    val temp: List<Double>,
    @SerialName("relative_humidity_2m")
    val humidity: List<Double>,
    @SerialName("apparent_temperature")
    val apparentTemp: List<Double>,
    @SerialName("wind_speed_10m")
    val windSpeed: List<Double>,
    @SerialName("precipitation_probability")
    val precipProb: List<Double>,
    @SerialName("weather_code")
    val weatherCode: List<Int>,
    @SerialName("visibility")
    val visibility: List<Double>,
    @SerialName("pressure_msl")
    val pressure: List<Double>
)

fun OpenMeteoForecast.toForecast(): Forecast {
    return Forecast(
        forecast = this.hourly.time.mapIndexed { index, time ->
            SingleForecast(
                time = ForecastValue(time, this.forecastUnits.time),
                temp = ForecastValue(this.hourly.temp[index], this.forecastUnits.temp),
                humidity = ForecastValue(this.hourly.humidity[index], this.forecastUnits.humidity),
                apparentTemp = ForecastValue(this.hourly.apparentTemp[index], this.forecastUnits.apparentTemp),
                windSpeed = ForecastValue(this.hourly.windSpeed[index], this.forecastUnits.windSpeed),
                precipProb = ForecastValue(this.hourly.precipProb[index], this.forecastUnits.precipProb),
                weatherCode = ForecastValue(this.hourly.weatherCode[index], this.forecastUnits.weatherCode),
                visibility = ForecastValue(this.hourly.visibility[index], this.forecastUnits.visibility),
                pressure = ForecastValue(this.hourly.pressure[index], this.forecastUnits.pressure)
            )
        }
    )
}