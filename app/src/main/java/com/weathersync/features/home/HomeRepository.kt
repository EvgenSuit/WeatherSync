package com.weathersync.features.home

import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.weather.GenerationType
import com.weathersync.utils.weather.Limit
import com.weathersync.utils.weather.LimitManager

class HomeRepository(
    private val limitManager: LimitManager,
    private val currentWeatherRepository: CurrentWeatherRepository,
    private val geminiRepository: GeminiRepository
) {
    suspend fun calculateLimit(): Limit = limitManager.calculateLimit(GenerationType.CurrentWeather)
    suspend fun recordTimestamp() = limitManager.recordTimestamp(GenerationType.CurrentWeather)
    suspend fun getCurrentWeather(isLimitReached: Boolean) = currentWeatherRepository.getCurrentWeather(isLimitReached)
    suspend fun generateSuggestions(
        isLimitReached: Boolean,
        currentWeather: CurrentWeather): Suggestions? =
        geminiRepository.generateSuggestions(isLimitReached = isLimitReached, currentWeather = currentWeather)
}