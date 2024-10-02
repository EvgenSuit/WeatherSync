package com.weathersync.features.home

import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.WeatherRepository

class HomeRepository(
    private val homeFirebaseClient: HomeFirebaseClient,
    private val weatherRepository: WeatherRepository,
    private val geminiRepository: GeminiRepository
) {
    suspend fun getCurrentWeather(): CurrentWeather {
        return weatherRepository.getCurrentWeather()
    }
    suspend fun generateSuggestions(currentWeather: CurrentWeather): Suggestions =
        geminiRepository.generateSuggestions(currentWeather)
}