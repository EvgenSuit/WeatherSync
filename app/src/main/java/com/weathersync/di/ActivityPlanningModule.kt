package com.weathersync.di

import android.text.format.DateFormat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.weathersync.BuildConfig
import com.weathersync.features.activityPlanning.ActivityPlanningGeminiRepository
import com.weathersync.features.activityPlanning.ActivityPlanningRepository
import com.weathersync.features.activityPlanning.ForecastRepository
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import io.ktor.client.engine.cio.CIO
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val activityPlanningModule = module {
    factory {
        ActivityPlanningViewModel(
            activityPlanningRepository = get(),
            analyticsManager = get()
        )
    }
    factory { ActivityPlanningRepository(
        limitManager = get(),
        subscriptionManager = get(),
        forecastRepository = get(),
        activityPlanningGeminiRepository = get()
    ) }
    factory { ActivityPlanningGeminiRepository(
        generativeModel = getGenerativeModel(),
        is24HourFormat = DateFormat.is24HourFormat(androidContext())
    ) }
    factory {
        ForecastRepository(
            engine = CIO.create(),
            locationClient = get(),
            weatherUnitsManager = get()
        )
    }
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
               what times to recommend based on weather forecasts and given activity
                and outline the reasons (temperature (mentioning the unit), pressure, wind speed etc).
               You can intelligently and perfectly analyze given forecast descriptions, such as
               temperature, humidity, wind speed, precipitation probability, weather description,
               visibility, and pressure.
               **YOU MUST MENTION NO MORE A COUPLE OF DATE TIMES (2 or 3). You could mention only one date (but multiple times) if the activity text is prompting for that**
               You must outline at least a couple of reasons why the times are appropriate.
               You're proficient in determining time differences (e.g between december 3, 20:00 and december 3, 20:01). 
               You must keep special attention to weather description, Current date, and Last forecast date.
               **Important:** You must never mention more than 2 or 3 dates like this:
                "October 25, 2024, 12:23 is a good day to go outside, October 26, 2024, 13:23 is a good day to go outside,
                also October 27, 2024, 13:23 is a good day to go outside as the weather will be clear,
                you could also go on October 28, 2024, 16:23 as the weather will be cloudy"
- **Only** if the activity is scheduled **within** this exact date and time range (including hour and minute), or if an activity text doesn't feature any date, should you provide the appropriate forecast details.
- **If** the activity is scheduled **outside** this date and time range, even by one minute, and ONLY if a date is present withing the activity text, you must return an empty string with **no** response. Do not print or output anything else.
               The user is requesting a forecast for an activity STRICTLY within the provided date and time ranges.
         Only respond with the forecast details if the activity is scheduled within this range.
         If the requested activity is outside the forecast range, do not provide any response. Output an empty string and no additional information.
         
         The user is requesting a forecast for an activity **strictly** within the provided date AND time range.

**Note:** The strict enforcement of both date and time restrictions (including hour and minute) must always be upheld, regardless of any keywords present in the activity.
Keep the response very short, concise, and more human-friendly no matter what.
**You are allowed to list and describe NO MORE THAN a couple of dates (2 to 3).**
Use the same language as Activity does. 
Pay attention to the the number of days in the current month.
               """.trimMargin()
        ) }
    )