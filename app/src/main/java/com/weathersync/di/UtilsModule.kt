package com.weathersync.di

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.weather.LimitManager
import com.weathersync.utils.weather.WeatherUnitsManager
import org.koin.dsl.module
import java.util.Locale

val utilsModule = module {
    factory { AnalyticsManager(auth = Firebase.auth, crashlytics = Firebase.crashlytics, analytics = Firebase.analytics) }
    factory { LimitManager(
        auth = Firebase.auth,
        firestore = Firebase.firestore,
        currentWeatherDAO = get(),
        weatherUpdater = get(),
        locale = Locale.getDefault()) }
    factory { WeatherUnitsManager(
        country = Locale.getDefault().country,
        auth = Firebase.auth,
        firestore = Firebase.firestore) }
}