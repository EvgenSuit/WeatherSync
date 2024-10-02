package com.weathersync.features.activityPlanning

import com.weathersync.features.activityPlanning.data.ForecastDates
import com.weathersync.features.activityPlanning.data.toForecast
import com.weathersync.utils.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ActivityPlanningRepository(
    private val weatherRepository: WeatherRepository,
    private val activityPlanningGeminiRepository: ActivityPlanningGeminiRepository
) {
    suspend fun generateTimes(activity: String): String {
        val openMeteoForecast = weatherRepository.getForecast(forecastDates = calculateForecastDays())
        val convertedForecast = openMeteoForecast.toForecast()
        val times = activityPlanningGeminiRepository.generateTimes(
            activity = activity,
            forecast =  convertedForecast)
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