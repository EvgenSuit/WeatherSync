package com.weathersync.common.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.weathersync.common.auth.mockAuth
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.ads.AdsDatastoreManager
import com.weathersync.utils.ads.adsDataStore
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk

fun mockAnalyticsManager(
    exceptionSlot: CapturingSlot<Exception>? = null,
    analytics: FirebaseAnalytics,
    adsDatastoreManager: AdsDatastoreManager
): AnalyticsManager =
    AnalyticsManager(auth = mockAuth(),
        crashlytics = mockk {
            every { isCrashlyticsCollectionEnabled = true } returns Unit
            every { log(any()) } returns Unit
            every { recordException(if (exceptionSlot != null) capture(exceptionSlot) else any()) } returns Unit
            every { setCustomKeys(any()) } returns Unit
            every { setCustomKey(any(), any<String>()) } returns Unit
        },
        analytics = analytics,
        adsDatastoreManager = adsDatastoreManager
    )