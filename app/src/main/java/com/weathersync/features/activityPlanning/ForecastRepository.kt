package com.weathersync.features.activityPlanning

import com.weathersync.features.activityPlanning.data.ForecastDates
import com.weathersync.features.activityPlanning.data.OpenMeteoForecast
import com.weathersync.features.home.LocationClient
import com.weathersync.utils.VisibilityUnit
import com.weathersync.utils.WeatherRepository
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get

class ForecastRepository(
    engine: HttpClientEngine,
    private val locationClient: LocationClient
): WeatherRepository {
    private val httpClient = getHttpClient(engine)

    // make this request to see what the forecast looks like in plain json:
    // https://api.open-meteo.com/v1/forecast?latitude=51.1&longitude=17.03333&start_hour=2024-09-30T23:00&end_hour=2024-10-01T23:00&timezone=Europe/Warsaw&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,precipitation_probability,weather_code,visibility,pressure_msl
    suspend fun getForecast(forecastDates: ForecastDates): OpenMeteoForecast {
        val coordinates = locationClient.getCoordinates()
        val requestUnits = timezoneToUnits()
        val requestUrl =
            "forecast?start_hour=${forecastDates.startDate}&end_hour=${forecastDates.endDate}" +
                    "&latitude=${coordinates.lat}&longitude=${coordinates.lon}" +
                    "&temperature_unit=${requestUnits.temp.symbol}&wind_speed_unit=${requestUnits.windSpeed.symbol}" +
                    "&timezone=$timezone&hourly=temperature_2m,relative_humidity_2m,apparent_temperature," +
                    "wind_speed_10m,precipitation_probability,weather_code,visibility,pressure_msl"
        val forecast = httpClient.get(requestUrl).body<OpenMeteoForecast>()
        return forecast.copy(
            forecastUnits = forecast.forecastUnits.copy(
                visibility = requestUnits.visibility.symbol
            ),
            hourly = forecast.hourly.copy(
                time = forecast.hourly.time.map(::convertToCorrectDateFormat),
                visibility = forecast.hourly.visibility.map { convertVisibility(it, requestUnits.visibility) }
            ),
            locality = coordinates.locality
        )
    }
    private fun convertVisibility(visibilityInMeters: Double, unit: VisibilityUnit): Double {
        return when (unit) {
            VisibilityUnit.KILOMETERS -> visibilityInMeters / 1000
            VisibilityUnit.MILES -> visibilityInMeters * 0.000621371
            VisibilityUnit.NAUTICAL_MILES -> visibilityInMeters * 0.000539957
            VisibilityUnit.METERS -> visibilityInMeters
        }.toDouble()
    }
}