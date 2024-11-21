package com.weathersync.common.weather

import android.location.Address
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.weathersync.utils.weather.LocationClient
import com.weathersync.features.home.getMockedWeather
import io.mockk.every
import io.mockk.mockk

data class LocationInfo(
    val city: String = "Wroclaw",
    val country: String = "Poland",
    val latitude: Double = 52.52,
    val longitude: Double = 13.405
)
val locationInfo = LocationInfo()
fun mockLocationClient(
    geocoderException: Exception? = null,
    lastLocationException: Exception? = null
): LocationClient {
    val location = mockk<Location> {
        every { latitude } returns locationInfo.latitude
        every { longitude } returns locationInfo.longitude
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
            every { locality } returns locationInfo.city
            every { countryName } returns locationInfo.country
        }
    }
    return LocationClient(
        fusedLocationProviderClient = fusedLocationProviderClient,
        geocoder = mockk {
            every { getFromLocation(getMockedWeather(fetchedWeatherUnits).latitude, getMockedWeather(
                fetchedWeatherUnits
            ).longitude, 1) } answers {
                if (geocoderException != null) throw geocoderException
                addresses
            }
        }
    )
}