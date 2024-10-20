package com.weathersync.features.activityPlanning

import com.weathersync.features.activityPlanning.data.ForecastDates
import com.weathersync.features.activityPlanning.data.toForecast
import com.weathersync.features.home.CurrentWeatherRepository
import com.weathersync.utils.GenerationType
import com.weathersync.utils.Limit
import com.weathersync.utils.LimitManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ActivityPlanningRepository(
    private val limitManager: LimitManager,
    private val forecastRepository: ForecastRepository,
    private val activityPlanningGeminiRepository: ActivityPlanningGeminiRepository
) {
    suspend fun calculateLimit(): Limit = limitManager.calculateLimit(GenerationType.ActivityRecommendations)
    suspend fun recordTimestamp() = limitManager.recordTimestamp(GenerationType.ActivityRecommendations)
    suspend fun generateRecommendations(activity: String): String {
        val openMeteoForecast = forecastRepository.getForecast(forecastDates = calculateForecastDays())
        val convertedForecast = openMeteoForecast.toForecast()
        val times = activityPlanningGeminiRepository.generateRecommendations(
            activity = activity,
            forecast = convertedForecast)
        return times
    }
    private fun calculateForecastDays(): ForecastDates {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val currDate = dateFormat.parse(dateFormat.format(System.currentTimeMillis()))
        val calendar = Calendar.getInstance()
        calendar.time = currDate!!

        calendar.add(Calendar.DAY_OF_MONTH, 5)
        val newDate = calendar.time
        return ForecastDates(startDate = dateFormat.format(currDate.time), endDate = dateFormat.format(newDate.time))
    }
}