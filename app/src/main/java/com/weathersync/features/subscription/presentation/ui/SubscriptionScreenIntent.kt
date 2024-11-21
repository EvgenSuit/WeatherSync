package com.weathersync.features.subscription.presentation.ui

import android.app.Activity

sealed class SubscriptionScreenIntent {
    data object FetchSubscriptionDetails: SubscriptionScreenIntent()
    data class Purchase(val activity: Activity): SubscriptionScreenIntent()
}