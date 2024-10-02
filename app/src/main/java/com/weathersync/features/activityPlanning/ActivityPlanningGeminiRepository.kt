package com.weathersync.features.activityPlanning

import com.google.ai.client.generativeai.GenerativeModel
import com.weathersync.features.activityPlanning.data.Forecast
import com.weathersync.utils.GeminiRepository
import com.weathersync.utils.weatherCodeToDescription
import java.time.ZoneId
import java.util.Locale

val activitiesPlanningTag = "[ACTIVITIES_PLANNING]"
class ActivityPlanningGeminiRepository(
    private val generativeModel: GenerativeModel
): GeminiRepository {
    suspend fun generateTimes(
        activity: String,
        forecast: Forecast): String {
        val forecastPrompt = constructForecastText(forecast)
        val prompt = constructPrompt(activity = activity.trim(),
            currentDatetime = forecast.forecast[0].time.value.toString(),
            lastForecastDatetime = forecast.forecast.last().time.value.toString(),
            forecastText = forecastPrompt)
        println(prompt)
        val plainText = generativeModel.generateContent(prompt).text ?: throw Exception("Empty response from Gemini \n" +
                "Prompt: $prompt")
        val extractedContent = extractContentWithTags(
            prompt = prompt,
            content = plainText,
            tags = listOf(activitiesPlanningTag))
        return extractedContent[0][0]
    }
    private fun constructPrompt(
        activity: String,
        currentDatetime: String,
        lastForecastDatetime: String,
        forecastText: String): String = """
        Forecast: $forecastText.
        Activity: $activity,
        Timezone: ${ZoneId.systemDefault()}.
        Current date: $currentDatetime. Last forecast date: $lastForecastDatetime.
        
        The user is requesting a forecast for an activity **strictly** within the date and time range from **$currentDatetime** to **$lastForecastDatetime**.
- **Only** if the activity is scheduled **within** this exact date and time range (including hour and minute), should you provide the appropriate forecast details.
- **If** the activity is scheduled **outside** this date and time range, **even by one minute**, you must return an empty string with **no** response. Do not print or output anything else.

**Note:** The strict enforcement of both date and time restrictions (including hour and minute) must always be upheld, regardless of any keywords present in the activity.
         
        Output WITHOUT numeration, *, #, dots at the end, and other symbols.
            You must print times in a format appropriate to the ${ZoneId.systemDefault()} timezone, and dates in the format like October 12, 2023.
            If the activity is not suitable for the weather, explain why.
            For each activity, if it involves a visit (e.g., visiting a dentist, friend, or location), suggest specific times based on weather suitability. 
            Use language: ${Locale.getDefault().language}.
            Your responsibility is to shortly recommend a couple of perfect times for an activity followed by reasons and corresponding dates and times
         based on weather forecast for ALL (and only) days in the forecast. EVERYTHING in the forecast data is of EXTREME importance.
         YOU MUST FOLLOW THIS FORMAT WITHOUT ANY EXCEPTIONS:
        $activitiesPlanningTag[activities go here]$activitiesPlanningTag.
         Keep the response very short, concise, and more human-friendly no matter what.
    """.trimIndent()

    private fun constructForecastText(forecast: Forecast): String =
        forecast.forecast.mapIndexed { i, singleForecast ->
            """$i. Time: ${singleForecast.time.value},
                temperature: ${singleForecast.temp.value} ${singleForecast.temp.unit},
                humidity: ${singleForecast.humidity.value} ${singleForecast.humidity.unit},
                wind speed: ${singleForecast.windSpeed.value} ${singleForecast.windSpeed.unit},
                precipitation probability: ${singleForecast.precipProb.value} ${singleForecast.precipProb.unit},
                weather description: ${weatherCodeToDescription(singleForecast.weatherCode.value as Int)},
                visibility: ${singleForecast.visibility.value} ${singleForecast.visibility.unit},
                pressure: ${singleForecast.pressure.value} ${singleForecast.pressure.unit}.
            """.trimMargin()
        }.joinToString("\n")


}