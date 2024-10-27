package com.weathersync.di

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.weathersync.utils.Country
import com.weathersync.utils.CrashlyticsManager
import com.weathersync.utils.LimitManager
import com.weathersync.utils.LimitManagerConfig
import com.weathersync.utils.WeatherUnitsManager
import org.koin.dsl.module
import java.util.Locale

val utilsModule = module {
    single { CrashlyticsManager(auth = Firebase.auth, crashlytics = Firebase.crashlytics, analytics = Firebase.analytics) }
    factory { LimitManager(
        limitManagerConfig = LimitManagerConfig(6, 6),
        auth = Firebase.auth,
        firestore = Firebase.firestore,
        currentWeatherDAO = get(),
        weatherUpdater = get()) }
    factory { WeatherUnitsManager(
        country = Locale.getDefault().country,
        auth = Firebase.auth,
        firestore = Firebase.firestore) }
}