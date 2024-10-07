package com.weathersync.di

import android.location.Geocoder
import androidx.room.Room
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.weathersync.BuildConfig
import com.weathersync.features.home.GeminiRepository
import com.weathersync.utils.LimitManager
import com.weathersync.features.home.HomeRepository
import com.weathersync.features.home.WeatherUpdater
import com.weathersync.features.home.LocationClient
import com.weathersync.features.home.data.db.CurrentWeatherLocalDB
import com.weathersync.utils.WeatherRepository
import com.weathersync.features.home.presentation.HomeViewModel
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
        weatherRepository = get(),
        geminiRepository = get()
    ) }
    single { LimitManager(auth = Firebase.auth, firestore = Firebase.firestore,
        currentWeatherDAO = get(),
        weatherUpdater = get()) }
    single { WeatherRepository(
        engine = CIO.create(),
        locationClient = get(),
        currentWeatherDAO = get(),
        weatherUpdater = get()
    ) }
    single { LocationClient(
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(androidContext()),
        geocoder = Geocoder(androidContext(), Locale.getDefault())
    ) }
    single { GeminiRepository(
        generativeModel = getGenerativeModel(),
        currentWeatherDAO = get(),
        weatherUpdater = get()
    ) }
    single { Room.databaseBuilder(
        androidContext(),
        CurrentWeatherLocalDB::class.java,
        "current_weather_database"
    ).fallbackToDestructiveMigration().build()
        .currentWeatherDao() }
    single { WeatherUpdater(clock = Clock.systemDefaultZone()) }
}

private fun getGenerativeModel(): GenerativeModel =
    GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            maxOutputTokens = 800
        }
    )