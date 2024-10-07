package com.weathersync.features.home

import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.GenerationType
import com.weathersync.utils.Limit
import com.weathersync.utils.LimitManager
import com.weathersync.utils.WeatherRepository

class HomeRepository(
    private val limitManager: LimitManager,
    private val weatherRepository: WeatherRepository,
    private val geminiRepository: GeminiRepository
) {
    suspend fun calculateLimit(): Limit = limitManager.calculateLimit(GenerationType.CurrentWeather)
    suspend fun getCurrentWeather(isLimitReached: Boolean): CurrentWeather? {
        return weatherRepository.getCurrentWeather(isLimitReached)
    }
    suspend fun generateSuggestions(
        isLimitReached: Boolean,
        currentWeather: CurrentWeather): Suggestions? =
        geminiRepository.generateSuggestions(isLimitReached = isLimitReached, currentWeather = currentWeather)
}