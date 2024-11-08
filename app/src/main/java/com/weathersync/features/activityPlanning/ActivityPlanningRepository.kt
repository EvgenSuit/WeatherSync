package com.weathersync.features.activityPlanning

import com.weathersync.features.activityPlanning.data.Forecast
import com.weathersync.features.activityPlanning.data.ForecastDates
import com.weathersync.features.activityPlanning.data.toForecast
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.weather.GenerationType
import com.weathersync.utils.weather.LimitManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ActivityPlanningRepository(
    private val limitManager: LimitManager,
    private val subscriptionManager: SubscriptionManager,
    private val forecastRepository: ForecastRepository,
    private val activityPlanningGeminiRepository: ActivityPlanningGeminiRepository
) {
    suspend fun isSubscribed() = subscriptionManager.initBillingClient()
    suspend fun calculateLimit(isSubscribed: IsSubscribed) =
        limitManager.calculateLimit(
            isSubscribed = isSubscribed,
            generationType = GenerationType.ActivityRecommendations)

    suspend fun recordTimestamp() = limitManager.recordTimestamp(GenerationType.ActivityRecommendations)

    suspend fun getForecast(isSubscribed: IsSubscribed): Forecast {
        val forecastDays = calculateForecastDays(isSubscribed = isSubscribed)
        val openMeteoForecast = forecastRepository.getForecast(
            forecastDates = forecastDays)
        return openMeteoForecast.toForecast(forecastDays = forecastDays.days)
    }
    suspend fun generateRecommendations(activity: String, forecast: Forecast) =
        activityPlanningGeminiRepository.generateRecommendations(
            activity = activity,
            forecast = forecast)

    private fun calculateForecastDays(isSubscribed: IsSubscribed): ForecastDates {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val currDate = dateFormat.parse(dateFormat.format(System.currentTimeMillis()))
        val calendar = Calendar.getInstance()
        calendar.time = currDate!!

        val days = if (isSubscribed) 15 else 5
        calendar.add(Calendar.DAY_OF_MONTH, days)
        val newDate = calendar.time
        return ForecastDates(
            startDate = dateFormat.format(currDate.time),
            endDate = dateFormat.format(newDate.time),
            days = days)
    }
}