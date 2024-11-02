package com.weathersync.di

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.weathersync.features.settings.SettingsRepository
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.themeDatastore
import com.weathersync.features.settings.presentation.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsModule = module {
    single { ThemeManager(dataStore = androidContext().themeDatastore) }
    factory { SettingsRepository(
        auth = Firebase.auth,
        themeManager = get(),
        weatherUnitsManager = get())
    }
    factory { SettingsViewModel(settingsRepository = get(), crashlyticsManager = get()) }
}