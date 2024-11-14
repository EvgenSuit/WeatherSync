package com.weathersync.di

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.ads.AdsDatastoreManager
import com.weathersync.utils.ads.adsDataStore
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.subscription.data.subscriptionInfoDatastore
import com.weathersync.utils.weather.LimitManager
import com.weathersync.utils.weather.NextUpdateTimeFormatter
import com.weathersync.utils.weather.WeatherUnitsManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.time.Clock
import java.time.ZoneId
import java.util.Locale

val utilsModule = module {
    factory { AnalyticsManager(
        auth = Firebase.auth,
        crashlytics = Firebase.crashlytics,
        analytics = Firebase.analytics,
        adsDatastoreManager = get()
        ) }
    factory { LimitManager(
        auth = Firebase.auth,
        firestore = Firebase.firestore,
        currentWeatherDAO = get(),
        weatherUpdater = get()) }
    factory { WeatherUnitsManager(
        country = Locale.getDefault().country,
        auth = Firebase.auth,
        firestore = Firebase.firestore) }
    factory { NextUpdateTimeFormatter(
        clock = Clock.system(ZoneId.systemDefault()),
        locale = Locale.getDefault()
    ) }
    single { SubscriptionManager(
        billingClientBuilder = BillingClient.newBuilder(androidContext())
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            ),
        subscriptionInfoDatastore = get(),
        analyticsManager = get(),
        adsDatastoreManager = get()
    ) }
    single { SubscriptionInfoDatastore(
        dataStore = androidContext().subscriptionInfoDatastore
    ) }
    single { AdsDatastoreManager(
        dataStore = androidContext().adsDataStore
    ) }
}