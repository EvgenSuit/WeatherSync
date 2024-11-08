package com.weathersync.features.home

import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.weather.GenerationType
import com.weathersync.utils.weather.Limit
import com.weathersync.utils.weather.LimitManager

class HomeRepository(
    private val limitManager: LimitManager,
    private val subscriptionManager: SubscriptionManager,
    private val currentWeatherRepository: CurrentWeatherRepository,
    private val geminiRepository: GeminiRepository
) {
    suspend fun isSubscribed() = subscriptionManager.initBillingClient()
    suspend fun calculateLimit(isSubscribed: IsSubscribed): Limit =
        limitManager.calculateLimit(
            isSubscribed = isSubscribed,
            generationType = GenerationType.CurrentWeather)
    suspend fun recordTimestamp() = limitManager.recordTimestamp(GenerationType.CurrentWeather)

    suspend fun getCurrentWeather(isLimitReached: Boolean) = currentWeatherRepository.getCurrentWeather(isLimitReached)
    suspend fun generateSuggestions(
        isLimitReached: Boolean,
        currentWeather: CurrentWeather): Suggestions? =
        geminiRepository.generateSuggestions(isLimitReached = isLimitReached, currentWeather = currentWeather)
}