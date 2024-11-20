package com.weathersync.di

import android.text.format.DateFormat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.weathersync.BuildConfig
import com.weathersync.features.activityPlanning.ActivityPlanningAIRepository
import com.weathersync.features.activityPlanning.ActivityPlanningRepository
import com.weathersync.features.activityPlanning.ForecastRepository
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import com.weathersync.utils.ai.gemini.GeminiClient
import com.weathersync.utils.ai.openai.OpenAIClient
import com.weathersync.utils.subscription.IsSubscribed
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
    factory { ActivityPlanningAIRepository(
        aiClientProvider = get(),
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
            """
               """.trimMargin()
        ) }
    )