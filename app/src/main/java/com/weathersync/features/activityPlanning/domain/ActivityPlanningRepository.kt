package com.weathersync.features.activityPlanning.domain

import com.weathersync.features.activityPlanning.ForecastDays
import com.weathersync.features.activityPlanning.ForecastRepository
import com.weathersync.features.activityPlanning.data.Forecast
import com.weathersync.features.activityPlanning.data.ForecastDates
import com.weathersync.features.activityPlanning.data.toForecast
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.weather.limits.GenerationType
import com.weathersync.utils.weather.limits.LimitManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ActivityPlanningRepository(
    private val limitManager: LimitManager,
    private val subscriptionManager: SubscriptionManager,
    private val forecastRepository: ForecastRepository,
    private val activityPlanningGeminiRepository: ActivityPlanningAIRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun isSubscribed() = subscriptionManager.initBillingClient()
    suspend fun calculateLimit(isSubscribed: IsSubscribed) =
        limitManager.calculateLimit(
            isSubscribed = isSubscribed,
            generationType = GenerationType.ActivityRecommendations)

    suspend fun recordTimestamp() = limitManager.recordTimestamp(GenerationType.ActivityRecommendations)

    suspend fun getForecast(isSubscribed: IsSubscribed): Forecast = withContext(dispatcher) {
        val forecastDays = calculateForecastDays(isSubscribed = isSubscribed)
        val openMeteoForecast = forecastRepository.getForecast(
            forecastDates = forecastDays)
        openMeteoForecast.toForecast(forecastDays = forecastDays.days)
    }
    suspend fun generateRecommendations(
        activity: String,
        isSubscribed: IsSubscribed,
        forecast: Forecast) = withContext(dispatcher) {
        activityPlanningGeminiRepository.generateRecommendations(
            activity = activity,
            isSubscribed = isSubscribed,
            forecast = forecast)
    }

    private fun calculateForecastDays(isSubscribed: IsSubscribed): ForecastDates {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val currDate = dateFormat.parse(dateFormat.format(System.currentTimeMillis()))
        val calendar = Calendar.getInstance()
        calendar.time = currDate!!

        val days = (if (isSubscribed) ForecastDays.PREMIUM else ForecastDays.REGULAR).days
        calendar.add(Calendar.DAY_OF_MONTH, days)
        val newDate = calendar.time
        return ForecastDates(
            startDate = dateFormat.format(currDate.time),
            endDate = dateFormat.format(newDate.time),
            days = days)
    }
}