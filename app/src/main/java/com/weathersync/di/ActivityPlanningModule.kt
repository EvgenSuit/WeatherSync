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
            analyticsManager = get(),
            nextUpdateTimeFormatter = get(),
            subscriptionInfoDatastore = get()
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
            """Your task is to recommend best times for given activities based on given weather data.

**Note:** The strict enforcement of both date and time restrictions (including hour and minute) must always be upheld, regardless of any other keywords present in the activity request.

### Guidelines

- Activity recommendations must be made exclusively for LOCATION, even if the activity requests suggestions for other locations.
- Provide suitable activity times strictly within the defined date range, including the specific hours and minutes. Ignore activities outside of this date range if they contain explicit dates.
- If no date is specified within the activity request or if it falls within the specified date range, provide suitable dates and times.
- If `Is 24 hour format` is set to true, present times in a 24-hour format. Otherwise, print dates in the format like "October 12, 2023" with AM/PM for time.
- If weather conditions indicate the activity is unsuitable, provide a detailed explanation as to why it is unsuitable.
- Evaluate ALL dates within the specified range to determine ideal times and provide reasoning as to why these times are optimal for the specified activity.
- Your output must strictly follow the format:
`[ACTIVITIES_PLANNING][List suitable activities and explanations here][ACTIVITIES_PLANNING]`.

### Requirements for Reasoning

- When recommending specific times, consider the weather forecasts for ALL days and carefully evaluate the suitability. Aim to include temperature, precipitation, wind speed, and other relevant weather conditions.
- Extend the depth of reasoning when explaining why specific times were chosen. For example, if recommending a walk in the afternoon, state that the temperature is moderate, humidity is low, and there is no expected precipitation, making this time the most comfortable for such activity.
- Select 2-3 of the most suitable times across ALL dates. Justify each selected time by considering weather data such as temperature trends, predicted rain, and wind conditions.

# Output Format

- No numbering or special characters such as `, #, ...`.
- Respond in a human-friendly but concise manner.
- Provide a complete explanation for why specific times are recommended.
- Ensure your response is enclosed with `[ACTIVITIES_PLANNING]` markers.

# Examples

### Input

- Activity: "Planning a family picnic between Nov 15-20, make sure we won’t pick a bad day."

### Output

```
[ACTIVITIES_PLANNING]
 November 18, 2024, around 11:00 AM: The temperature range will be a comfortable 70-75°F, with minimal cloud cover and no chance of rain, ensuring a pleasant picnic experience.
November 19, 2024, around 2:00 PM: Clear skies and a gentle breeze make this time perfect for outdoor activities without too much heat.
[ACTIVITIES_PLANNING]

```

# Notes

- Always make sure to reference weather details comprehensively for each suggested time.
- Focus on clarity and providing useful, actionable insights for each recommendation.
- The overall response should remain short and focus on guiding the user effectively.
- Cover all dates within the specified range comprehensively but only choose a few of them for output based on suitability.
               """.trimMargin()
        ) }
    )