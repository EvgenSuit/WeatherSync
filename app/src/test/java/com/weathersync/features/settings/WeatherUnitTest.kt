package com.weathersync.features.settings

interface WeatherUnitTest {
    fun fetchUnits_success()
    fun fetchUnits_exception()

    fun setUnits_success()
    fun setUnits_exception()
}