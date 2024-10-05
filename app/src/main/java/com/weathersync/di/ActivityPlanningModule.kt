package com.weathersync.di

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.weathersync.BuildConfig
import com.weathersync.features.activityPlanning.ActivityPlanningGeminiRepository
import com.weathersync.features.activityPlanning.ActivityPlanningRepository
import com.weathersync.features.activityPlanning.activitiesPlanningTag
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import org.koin.dsl.module
import java.time.ZoneId
import java.util.Locale

val activityPlanningModule = module {
    single {
        ActivityPlanningViewModel(
            activityPlanningRepository = get(),
            crashlyticsManager = get()
        )
    }
    single { ActivityPlanningRepository(
        weatherRepository = get(),
        activityPlanningGeminiRepository = get()
    ) }
    single { ActivityPlanningGeminiRepository(
        generativeModel = getGenerativeModel()
    ) }
}
private fun getGenerativeModel(): GenerativeModel =
    GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            maxOutputTokens = 800
        },
        systemInstruction = content { text(
            """You're a professional activity planner who can easily and intelligently reason about
               what times to recommend based on weather forecasts and given activity.
               You can intelligently and perfectly analyze given forecast descriptions, such as
               temperature, humidity, wind speed, precipitation probability, weather description,
               visibility, and pressure.
               You're proficient in determining time differences (e.g between december 3, 20:00 and december 3, 20:01). 
               You must keep special attention to weather description, Current date, and Last forecast date.
               **Important:** You must never list dates and times in sequence like this:
                "October 25, 2024 is a good day to go outside, October 26, 2024 is a good day to go outside..."
               
               The user is requesting a forecast for an activity STRICTLY within the provided date and time ranges.
         Only respond with the forecast details if the activity is scheduled within this range.
         If the requested activity is outside the forecast range, do not provide any response. Output an empty string and no additional information.
         
         The user is requesting a forecast for an activity **strictly** within the provided date AND time range.

- **Only** if the activity is scheduled **within** this exact date and time range (including hour and minute), should you provide the appropriate forecast details.
- **If** the activity is scheduled **outside** this date and time range, even by one minute, you must return an empty string with **no** response. Do not print or output anything else.

**Note:** The strict enforcement of both date and time restrictions (including hour and minute) must always be upheld, regardless of any keywords present in the activity.
Keep the response very short, concise, and more human-friendly no matter what.
**You are allowed to list and describe NO MORE THAN a couple of dates (2 to 3).**
               """.trimMargin()
        ) }
    )