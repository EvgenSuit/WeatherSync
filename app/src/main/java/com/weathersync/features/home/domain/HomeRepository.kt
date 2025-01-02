package com.weathersync.features.home.domain

import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.weather.limits.QueryType
import com.weathersync.utils.weather.limits.Limit
import com.weathersync.utils.weather.limits.LimitManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepository(
    private val limitManager: LimitManager,
    private val subscriptionManager: SubscriptionManager,
    private val currentWeatherRepository: CurrentWeatherRepository,
    private val homeAIRepository: HomeAIRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun isSubscribed() = subscriptionManager.initBillingClient()
    suspend fun calculateLimit(isSubscribed: IsSubscribed,
                               refresh: Boolean): Limit = withContext(dispatcher) {
        limitManager.calculateLimit(
            isSubscribed = isSubscribed,
            queryType = QueryType.CurrentWeather(refresh))
    }
    suspend fun recordTimestamp() = limitManager.recordTimestamp(QueryType.CurrentWeather(null))

    suspend fun getCurrentWeather(isLimitReached: Boolean) =
        withContext(dispatcher) {
            currentWeatherRepository.getCurrentWeather(isLimitReached)
        }
    suspend fun insertWeatherAndSuggestions(
        currentWeather: CurrentWeather,
        suggestions: Suggestions) = withContext(dispatcher) {
        currentWeatherRepository.insertWeather(currentWeather)
        homeAIRepository.insertSuggestions(suggestions)
    }
    suspend fun generateSuggestions(
        isLimitReached: Boolean,
        isSubscribed: IsSubscribed,
        currentWeather: CurrentWeather): Suggestions = withContext(dispatcher) {
        homeAIRepository.generateSuggestions(
            isLimitReached = isLimitReached,
            isSubscribed = isSubscribed,
            currentWeather = currentWeather)
    }
}