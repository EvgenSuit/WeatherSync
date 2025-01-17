package com.weathersync.di

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.TimeAPI
import com.weathersync.utils.ads.AdsDatastoreManager
import com.weathersync.utils.ads.adsDataStore
import com.weathersync.utils.ai.AIClientProvider
import com.weathersync.utils.ai.gemini.GeminiClient
import com.weathersync.utils.ai.openai.OpenAIClient
import com.weathersync.utils.appReview.AppReviewManager
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.subscription.data.subscriptionInfoDatastore
import com.weathersync.utils.weather.WeatherUnitsManager
import com.weathersync.utils.weather.limits.LimitManager
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import io.ktor.client.engine.cio.CIO
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.time.Clock
import java.util.Locale

val utilsModule = module {
    factory { AnalyticsManager(
        auth = Firebase.auth,
        crashlytics = Firebase.crashlytics,
        analytics = Firebase.analytics,
        adsDatastoreManager = get()
        ) }
    single { TimeAPI(engine = CIO.create()) }
    factory { LimitManager(
        auth = Firebase.auth,
        firestore = Firebase.firestore,
        currentWeatherDAO = get(),
        weatherUpdater = get(),
        timeAPI = get()) }
    factory { WeatherUnitsManager(
        country = Locale.getDefault().country,
        auth = Firebase.auth,
        firestore = Firebase.firestore) }
    factory { NextUpdateTimeFormatter(
        clock = Clock.systemDefaultZone(),
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
    single { AIClientProvider(
        openAIClient = OpenAIClient(CIO.create()),
        geminiClient = GeminiClient(CIO.create())
    ) }
    single { AppReviewManager(
        timeAPI = get(),
        auth = Firebase.auth,
        firestore = Firebase.firestore,
        manager = ReviewManagerFactory.create(androidContext())
    ) }
}