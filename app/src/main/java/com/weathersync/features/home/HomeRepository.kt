package com.weathersync.features.home

import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.weather.limits.GenerationType
import com.weathersync.utils.weather.limits.Limit
import com.weathersync.utils.weather.limits.LimitManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepository(
    private val limitManager: LimitManager,
    private val subscriptionManager: SubscriptionManager,
    private val currentWeatherRepository: CurrentWeatherRepository,
    private val geminiRepository: GeminiRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun isSubscribed() = subscriptionManager.initBillingClient()
    suspend fun calculateLimit(isSubscribed: IsSubscribed): Limit = withContext(dispatcher) {
        limitManager.calculateLimit(
            isSubscribed = isSubscribed,
            generationType = GenerationType.CurrentWeather)
    }
    suspend fun recordTimestamp() = limitManager.recordTimestamp(GenerationType.CurrentWeather)

    suspend fun getCurrentWeather(isLimitReached: Boolean) =
        withContext(dispatcher) {
            currentWeatherRepository.getCurrentWeather(isLimitReached)
        }
    suspend fun generateSuggestions(
        isLimitReached: Boolean,
        currentWeather: CurrentWeather): Suggestions? = withContext(dispatcher) {
        geminiRepository.generateSuggestions(isLimitReached = isLimitReached, currentWeather = currentWeather)
    }
}