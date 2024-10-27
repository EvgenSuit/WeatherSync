package com.weathersync.di

import com.weathersync.features.settings.SettingsRepository
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.themeDatastore
import com.weathersync.features.settings.presentation.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsModule = module {
    single { ThemeManager(dataStore = androidContext().themeDatastore) }
    single { SettingsRepository(
        themeManager = get(),
        weatherUnitsManager = get())
    }
    single { SettingsViewModel(settingsRepository = get(), crashlyticsManager = get()) }
}