package com.weathersync.di

import android.location.Geocoder
import androidx.room.Room
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.android.gms.location.LocationServices
import com.weathersync.BuildConfig
import com.weathersync.features.home.GeminiRepository
import com.weathersync.features.home.HomeRepository
import com.weathersync.features.home.LocationClient
import com.weathersync.features.home.WeatherUpdater
import com.weathersync.features.home.data.db.CurrentWeatherLocalDB
import com.weathersync.features.home.presentation.HomeViewModel
import com.weathersync.features.home.CurrentWeatherRepository
import io.ktor.client.engine.cio.CIO
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.time.Clock
import java.util.Locale

val homeModule = module {
    single { HomeViewModel(
        homeRepository = get(),
        crashlyticsManager = get()
    ) }
    single { HomeRepository(
        limitManager = get(),
        currentWeatherRepository = get(),
        geminiRepository = get()
    ) }
    single { CurrentWeatherRepository(
        engine = CIO.create(),
        locationClient = get(),
        currentWeatherDAO = get()
    ) }
    single { LocationClient(
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(androidContext()),
        geocoder = Geocoder(androidContext(), Locale.getDefault())
    ) }
    single { GeminiRepository(
        generativeModel = getGenerativeModel(),
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