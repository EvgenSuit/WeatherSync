package com.weathersync.features.home

import com.weathersync.features.home.data.Coordinates
import com.weathersync.features.home.data.CurrentOpenMeteoWeather
import com.weathersync.features.home.data.CurrentWeather

class HomeRepository(
    private val homeFirebaseClient: HomeFirebaseClient,
    private val weatherRepository: WeatherRepository
) {
    suspend fun getCurrentWeather(): CurrentWeather {
        return weatherRepository.getCurrentWeather()
    }
}