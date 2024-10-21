package com.weathersync.di

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.weathersync.features.settings.SettingsRepository
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.themeDatastore
import com.weathersync.features.settings.presentation.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsModule = module {
    single { ThemeManager(dataStore = androidContext().themeDatastore) }
    single { SettingsRepository(auth = Firebase.auth, firestore = Firebase.firestore,
        themeManager = get()) }
    single { SettingsViewModel(settingsRepository = get(), crashlyticsManager = get()) }
}