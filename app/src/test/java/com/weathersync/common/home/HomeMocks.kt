package com.weathersync.common.home

import com.weathersync.common.utils.locationInfo
import com.weathersync.features.home.data.CurrWeather
import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeatherUnits
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun mockCurrentWeatherEngine(
    status: HttpStatusCode
): HttpClientEngine = MockEngine { request ->
    val jsonResponse = Json.encodeToString(mockedWeather)
    respond(
        content = ByteReadChannel(jsonResponse),
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}
val mockedWeather = CurrentOpenMeteoWeather(
    latitude = locationInfo.latitude,
    longitude = locationInfo.longitude,
    timezone = "GMT",
    elevation = 34.0,
    currentWeatherUnits = CurrentWeatherUnits(
        time = "iso8601",
        interval = "seconds",
        temperature = "°C",
        windSpeed = "km/h",
        windDirection = "°"
    ),
    currentWeather = CurrWeather(
        time = "2023-09-26T12:00:00Z",
        interval = 900,
        temperature = 20.0,
        windSpeed = 15.0,
        windDirection = 270,
        isDay = 1,
        weatherCode = 2
    )
)
