package com.weathersync.di

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.weathersync.features.subscription.SubscriptionInfoRepository
import com.weathersync.features.subscription.presentation.SubscriptionInfoViewModel
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.subscription.data.subscriptionInfoDatastore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val subscriptionModule = module {
    single { SubscriptionInfoDatastore(
        dataStore = androidContext().subscriptionInfoDatastore
    ) }
    single { SubscriptionInfoRepository(subscriptionManager = get()) }
    factory { SubscriptionInfoViewModel(
        subscriptionInfoRepository = get(),
        analyticsManager = get()
    ) }
    single { SubscriptionManager(
        billingClientBuilder = BillingClient.newBuilder(androidContext())
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            ),
        subscriptionInfoDatastore = get(),
        analyticsManager = get()
    ) }
}