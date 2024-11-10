package com.weathersync.common.utils

import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.SubscriptionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf


fun mockSubscriptionManager(
    isSubscribed: IsSubscribed
): SubscriptionManager =
    mockk {
        coEvery { initBillingClient() } returns isSubscribed
        coEvery { isSubscribedFlow() } returns flowOf(isSubscribed)
    }