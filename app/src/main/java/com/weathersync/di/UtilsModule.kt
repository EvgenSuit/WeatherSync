package com.weathersync.di

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.weathersync.utils.CoroutineScopeProvider
import com.weathersync.utils.CrashlyticsManager
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val utilsModule = module {
    single { CoroutineScopeProvider(dispatcher = Dispatchers.IO) }
    single { CrashlyticsManager(auth = Firebase.auth, crashlytics = Firebase.crashlytics, analytics = Firebase.analytics) }
}