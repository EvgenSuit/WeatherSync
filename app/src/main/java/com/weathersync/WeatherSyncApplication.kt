package com.weathersync

import android.app.Application
import com.weathersync.di.authModule
import com.weathersync.di.homeModule
import com.weathersync.di.utilsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class WeatherSyncApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@WeatherSyncApplication)
            modules(
                authModule,
                utilsModule,
                homeModule
            )
        }
    }
}