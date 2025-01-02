package com.weathersync.utils.weather.location.data

import androidx.annotation.Keep

@Keep
data class LocationPreference(
    val location: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)
