package com.weathersync.common.home

import android.location.Address
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.weathersync.common.mockTask
import com.weathersync.features.home.LocationClient
import com.weathersync.features.home.data.CurrWeather
import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.CurrentWeatherUnits
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val city = "Wroclaw"
val country = "Poland"
fun mockLocationClient(
    geocoderException: Exception? = null,
    lastLocationException: Exception? = null
): LocationClient {
    val location = mockk<Location> {
        every { latitude } returns mockedWeather.latitude
        every { longitude } returns mockedWeather.longitude
    }
    val task = mockk<Task<Location>>()
    every { task.addOnSuccessListener(any()) } answers {
        val listener = it.invocation.args[0] as OnSuccessListener<Location>
        if (lastLocationException == null) listener.onSuccess(location)
        task
    }
    every { task.addOnFailureListener(any()) } answers {
        val listener = it.invocation.args[0] as OnFailureListener
        if (lastLocationException != null) listener.onFailure(lastLocationException)
        task
    }
    val fusedLocationProviderClient = mockk<FusedLocationProviderClient> {
        every { lastLocation } returns task
    }
    val addresses = mockk<List<Address>> {
        every { get(0) } returns mockk {
            every { locality } returns city
            every { countryName } returns country
        }
    }
    return LocationClient(
        fusedLocationProviderClient = fusedLocationProviderClient,
        geocoder = mockk {
            every { getFromLocation(mockedWeather.latitude, mockedWeather.longitude, 1) } answers {
                if (geocoderException != null) throw geocoderException
                addresses
            }
        }
    )
}

fun mockEngine(
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
    latitude = 52.52,
    longitude = 13.405,
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
