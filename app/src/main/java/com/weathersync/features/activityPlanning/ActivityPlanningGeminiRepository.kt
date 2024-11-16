package com.weathersync.features.activityPlanning

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.weathersync.features.activityPlanning.data.Forecast
import com.weathersync.utils.EmptyGeminiResponse
import com.weathersync.utils.weather.GeminiRepository
import com.weathersync.utils.weather.weatherCodeToDescription
import java.time.ZoneId

const val activitiesPlanningTag = "[ACTIVITIES_PLANNING]"
class ActivityPlanningGeminiRepository(
    private val generativeModel: GenerativeModel,
    private val is24HourFormat: Boolean
): GeminiRepository {
    suspend fun generateRecommendations(
        activity: String,
        forecast: Forecast): String {
        val forecastPrompt = constructForecastText(forecast)
        val prompt = constructPrompt(
            locality = forecast.locality,
            is24HourFormat = is24HourFormat,
            activity = activity.trim(),
            currentDatetime = forecast.forecast[0].time.value.toString(),
            lastForecastDatetime = forecast.forecast.last().time.value.toString(),
            forecastText = forecastPrompt)
        val plainText = generativeModel.generateContent(prompt).text ?: throw EmptyGeminiResponse("Empty response from Gemini \n" +
                "Prompt: $prompt")
        val extractedContent = extractContentWithTags(
            prompt = prompt,
            content = plainText,
            tags = listOf(activitiesPlanningTag))
        return extractedContent[0][0]
    }
    private fun constructPrompt(
        locality: String,
        is24HourFormat: Boolean,
        activity: String,
        currentDatetime: String,
        lastForecastDatetime: String,
        forecastText: String): String = """
        Actual locality: $locality
        Forecast: $forecastText.
        Activity: $activity,
        Timezone: ${ZoneId.systemDefault()}.
        Current date: $currentDatetime. Last forecast date: $lastForecastDatetime.
        
        The user is requesting a forecast for an activity **strictly** within the date and time range from **$currentDatetime** to **$lastForecastDatetime**,
        and strictly for this locality: $locality.

        Is 24 hour format: $is24HourFormat.
    """.trimIndent()

    private fun constructForecastText(forecast: Forecast): String =
        forecast.forecast.mapIndexed { i, singleForecast ->
            /*"""$i. Time: ${singleForecast.time.value},
                temperature: ${singleForecast.temp.value} ${singleForecast.temp.unit},
                humidity: ${singleForecast.humidity.value} ${singleForecast.humidity.unit},
                wind speed: ${singleForecast.windSpeed.value} ${singleForecast.windSpeed.unit},
                precipitation probability: ${singleForecast.precipProb.value} ${singleForecast.precipProb.unit},
                weather description: ${weatherCodeToDescription(singleForecast.weatherCode.value as Int)},
                visibility: ${singleForecast.visibility.value} ${singleForecast.visibility.unit},
                pressure: ${singleForecast.pressure.value} ${singleForecast.pressure.unit}.
            """.trimMargin()*/
            "$i. Time: ${singleForecast.time.value}, " +
            "temperature: ${singleForecast.temp.value} ${singleForecast.temp.unit}, " +
            "humidity: ${singleForecast.humidity.value} ${singleForecast.humidity.unit}, " +
            "wind speed: ${singleForecast.windSpeed.value} ${singleForecast.windSpeed.unit}, " +
            "precipitation probability: ${singleForecast.precipProb.value} ${singleForecast.precipProb.unit}, " +
            "weather description: ${weatherCodeToDescription(singleForecast.weatherCode.value as Int)}, " +
            "visibility: ${singleForecast.visibility.value} ${singleForecast.visibility.unit}, " +
            "pressure: ${singleForecast.pressure.value} ${singleForecast.pressure.unit}."
        }.joinToString("\n")


}