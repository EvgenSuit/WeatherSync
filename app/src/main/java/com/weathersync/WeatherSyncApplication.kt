package com.weathersync

import android.app.Application
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.ktx.Firebase
import com.weathersync.di.activityPlanningModule
import com.weathersync.di.authModule
import com.weathersync.di.homeModule
import com.weathersync.di.navModule
import com.weathersync.di.settingsModule
import com.weathersync.di.subscriptionModule
import com.weathersync.di.utilsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class WeatherSyncApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance()
            else PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@WeatherSyncApplication)
            modules(
                subscriptionModule,
                navModule,
                authModule,
                utilsModule,
                homeModule,
                activityPlanningModule,
                settingsModule
            )
        }
    }
}