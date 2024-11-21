package com.weathersync.features.subscription.domain

import android.app.Activity
import com.weathersync.utils.subscription.SubscriptionManager

class SubscriptionInfoRepository(
    private val subscriptionManager: SubscriptionManager
) {
    val purchasesUpdatedEvent = subscriptionManager.purchasesUpdatedEvent
    suspend fun isBillingSetupFinished() = subscriptionManager.initBillingClient()
    fun isSubscribedFlow() = subscriptionManager.isSubscribedFlow()
    suspend fun getSubscriptionDetails() = subscriptionManager.processPurchases()
    suspend fun purchase(activity: Activity) = subscriptionManager.purchase(activity)
}