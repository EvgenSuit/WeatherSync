package com.weathersync.features.activityPlanning

import com.weathersync.features.activityPlanning.data.ForecastDates
import com.weathersync.features.activityPlanning.data.OpenMeteoForecast
import com.weathersync.features.home.LocationClient
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.weather.WeatherUnitsManager
import com.weathersync.utils.weather.WeatherRepository
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get

class ForecastRepository(
    engine: HttpClientEngine,
    private val locationClient: LocationClient,
    private val weatherUnitsManager: WeatherUnitsManager
): WeatherRepository {
    private val httpClient = getHttpClient(engine)

    // make this request to see what a forecast looks like in plain json:
    // https://api.open-meteo.com/v1/forecast?latitude=51.1&longitude=17.03333&temperature_unit=fahrenheit&start_hour=2024-09-30T23:00&end_hour=2024-10-01T23:00&timezone=Europe/Warsaw&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,precipitation_probability,weather_code,visibility,pressure_msl
    suspend fun getForecast(forecastDates: ForecastDates): OpenMeteoForecast {
        val coordinates = locationClient.getCoordinates()
        val fetchedUnits = weatherUnitsManager.getUnits()
        // all parameters except temperature, wind speed and visibility are available in only 1 unit format
        val requestUrl =
            "forecast?start_hour=${forecastDates.startDate}&end_hour=${forecastDates.endDate}" +
                    "&latitude=${coordinates.lat}&longitude=${coordinates.lon}" +
                    "&temperature_unit=${fetchedUnits.temp.weatherApiUnitName}&wind_speed_unit=${fetchedUnits.windSpeed.weatherApiUnitName}" +
                    "&timezone=$timezone&hourly=temperature_2m,relative_humidity_2m,apparent_temperature," +
                    "wind_speed_10m,precipitation_probability,weather_code,visibility,pressure_msl"
        val forecast = httpClient.get(requestUrl).body<OpenMeteoForecast>()
        return forecast.copy(
            forecastUnits = forecast.forecastUnits.copy(
                visibility = fetchedUnits.visibility.unitName
            ),
            hourly = forecast.hourly.copy(
                time = forecast.hourly.time.map(::convertToCorrectDateFormat),
                // manual conversion is required since open weather api doesn't provide it
                visibility = forecast.hourly.visibility.map { convertVisibility(it, fetchedUnits.visibility) }
            ),
            locality = coordinates.locality
        )
    }
    private fun convertVisibility(visibilityInMeters: Double, unit: WeatherUnit.Visibility): Double {
        return when (unit) {
            WeatherUnit.Visibility.Kilometers -> visibilityInMeters / 1000
            WeatherUnit.Visibility.Miles -> visibilityInMeters * 0.000621371
            WeatherUnit.Visibility.Meters -> visibilityInMeters
        }.toDouble().round(1)
    }
}