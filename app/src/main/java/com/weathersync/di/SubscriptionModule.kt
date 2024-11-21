package com.weathersync.di

import com.weathersync.features.subscription.domain.SubscriptionInfoRepository
import com.weathersync.features.subscription.presentation.SubscriptionInfoViewModel
import org.koin.dsl.module

val subscriptionModule = module {
    single { SubscriptionInfoRepository(subscriptionManager = get()) }
    factory { SubscriptionInfoViewModel(
        subscriptionInfoRepository = get(),
        analyticsManager = get()
    ) }
}