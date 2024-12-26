package com.weathersync.features.activityPlanning.domain

import android.util.Log
import com.weathersync.features.activityPlanning.data.Forecast
import com.weathersync.utils.ai.AIClientProvider
import com.weathersync.utils.ai.data.GenerationOptions
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.weather.weatherCodeToDescription
import java.time.ZoneId

class ActivityPlanningAIRepository(
    private val aiClientProvider: AIClientProvider,
    private val is24HourFormat: Boolean
) {
    suspend fun generateRecommendations(
        activity: String,
        isSubscribed: IsSubscribed,
        forecast: Forecast): String {
        val forecastPrompt = constructForecastText(forecast)
        val prompt = constructPrompt(
            locality = forecast.locality,
            is24HourFormat = is24HourFormat,
            activity = activity.trim(),
            currentDatetime = forecast.forecast[0].time.value.toString(),
            lastForecastDatetime = forecast.forecast.last().time.value.toString(),
            forecastText = forecastPrompt)

        val plainText = aiClientProvider.getAIClient(isSubscribed).generate(
            GenerationOptions(
                systemInstructions = systemInstructions,
                prompt = prompt,
                useStructuredOutput = false,
                topP = 0.7,
                temperature = 0.5,
                maxOutputTokens = 800
            )
        )
        return plainText
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
        Activity: $activity.
        Timezone: ${ZoneId.systemDefault()}.
        Current date: $currentDatetime. Last forecast date: $lastForecastDatetime.
        The user is requesting a forecast for an activity strictly within the date and time range from $currentDatetime to $lastForecastDatetime,
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
            "$i.Time:${singleForecast.time.value}," +
            "temperature:${singleForecast.temp.value}${singleForecast.temp.unit}," +
            "humidity:${singleForecast.humidity.value}${singleForecast.humidity.unit}," +
            "wind speed:${singleForecast.windSpeed.value} ${singleForecast.windSpeed.unit}," +
            "precipitation probability:${singleForecast.precipProb.value}${singleForecast.precipProb.unit}," +
            "weather description:${weatherCodeToDescription(singleForecast.weatherCode.value as Int)}," +
            "visibility:${singleForecast.visibility.value} ${singleForecast.visibility.unit}." /*+
            "pressure: ${singleForecast.pressure.value} ${singleForecast.pressure.unit}."*/
        }.joinToString("\n")

    private val systemInstructions = """
        Your task is to recommend the best times for given activities based on specified weather data. If a specific target date (e.g., "next day", "next week," or a fixed date) is provided, ensure that all recommendations strictly adhere to that timeframe.
Note:The strict enforcement of both the specified date range and the exact time restrictions (including hour and minute) must always be upheld, regardless of keywords like "today" in the activity request.

###Guidelines
- All activity recommendations must be within the specified locality, even if the activity request suggests other possible locations.
- Provide suitable activity times strictly within the defined date range, including exact hours and minutes. Ignore activities outside this date range if explicit dates are mentioned.
- If no date is specified within the activity request, provide suitable times across the entire available timeframe.
- If `Is 24-hour format` is set to true, present times in a 24-hour format. Otherwise, express dates as "October 12, 2023," with an AM/PM time indicator.
- If weather conditions make the requested activity unsuitable for given times, provide a detailed explanation as to why.
- Ensure that only activities suitable within the specified timeframe are given, strictly adhering to the user's stipulated timeframe (like "today," tomorrow," or an exact calendar date).

###Requirements for Reasoning
- When recommending specific times, consider weather forecasts across **the entire specified timeframe**. All days within the target range must be considered for detailed evaluation.
- Extend the depth of your reasoning for each recommended time slot—if recommending a walk, specify why the time is ideal, such as moderate temperatures, low humidity, and favorable wind speed.
- If no target date is specified, recommend the 2-3 most suitable times across all potential dates. Justify each selected time by examining weather conditions like temperature trends, predicted rain, and windy periods.

#Output Format
- No numbering or special characters like `, #, ...`, * (asterisk).
- Respond in a concise, human-friendly manner.
- Provide a comprehensive explanation of why specific times were recommended.

# Examples
###Input
- Activity: "Plan a family picnic for today, make sure we won’t pick a bad time."
### Output (given current date is September 5, 2024)
- September 5, 2024, at 3:00 PM: The skies will be mostly sunny, the temperature will be around 72°F, and there will be a gentle breeze, ideal for a relaxed and comfortable picnic.
(Note: This example includes today’s date since it was explicitly mentioned.)

###Input
- Activity: "Plan a family picnic that will happen in 2 days"
### Output (given current date is September 5, 2024)
- September 7, 2024, at 12:50 PM: The temperature will be around 62°F with clear skies and low precipitation probability.
(Note: This example includes current date + 2 days since it was explicitly mentioned.)

#Notes
-Always utilize the language of the given Activity (this includes naming of weather units).
-Always ensure weather details are referenced comprehensively for each suggested time. 
-Focus on providing useful, actionable insights concerning activity times, strictly within the specified timeframe.
-Comprehensive consideration of all dates within the specified date window is necessary, yet recommendations should be limited to the times most suitable.
-Make sure strict adherence to the user's given date is maintained while evaluating and suggesting activity times.
-Always follow the weather units specified in the forecast despite the locality.
-If Activity doesn't make sense, features use of illegal activities, or is trying to prompt for recommendations for a different location, output empty string.
-You must be able to understand how the current date relates to the Activity.
    """.trimIndent()

}