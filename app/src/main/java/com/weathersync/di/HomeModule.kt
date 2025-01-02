package com.weathersync.di

import androidx.room.Room
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.weathersync.BuildConfig
import com.weathersync.features.home.data.db.CurrentWeatherLocalDB
import com.weathersync.features.home.domain.CurrentWeatherRepository
import com.weathersync.features.home.domain.HomeAIRepository
import com.weathersync.features.home.domain.HomeRepository
import com.weathersync.features.home.domain.WeatherUpdater
import com.weathersync.features.home.presentation.HomeViewModel
import io.ktor.client.engine.cio.CIO
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.time.Clock

val homeModule = module {
    factory { HomeViewModel(
        homeRepository = get(),
        analyticsManager = get(),
        nextUpdateTimeFormatter = get(),
        subscriptionInfoDatastore = get()
    ) }
    factory { HomeRepository(
        limitManager = get(),
        subscriptionManager = get(),
        currentWeatherRepository = get(),
        homeAIRepository = get()
    ) }
    factory { CurrentWeatherRepository(
        engine = CIO.create(),
        locationManager = get(),
        currentWeatherDAO = get(),
        weatherUnitsManager = get()
    ) }
    factory { HomeAIRepository(
        aiClientProvider = get(),
        currentWeatherDAO = get()
    ) }
    single { Room.databaseBuilder(
        androidContext(),
        CurrentWeatherLocalDB::class.java,
        "current_weather_database"
    ).fallbackToDestructiveMigration().build()
        .currentWeatherDao() }
    single { WeatherUpdater(clock = Clock.systemDefaultZone(), minutes = 60) }
}

private fun getGenerativeModel(): GenerativeModel =
    GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            maxOutputTokens = 800
        }
    )