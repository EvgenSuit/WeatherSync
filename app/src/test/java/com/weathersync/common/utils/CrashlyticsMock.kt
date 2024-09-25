package com.weathersync.common.utils

import com.weathersync.common.auth.mockAuth
import com.weathersync.utils.CrashlyticsManager
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk

fun mockCrashlyticsManager(
    exceptionSlot: CapturingSlot<Exception>? = null
): CrashlyticsManager =
    CrashlyticsManager(auth = mockAuth(),
        crashlytics = mockk {
            every { isCrashlyticsCollectionEnabled = any() } returns Unit
            every { log(any()) } returns Unit
            every { recordException(if (exceptionSlot != null) capture(exceptionSlot) else any()) } returns Unit
            every { setCustomKeys(any()) } returns Unit
            every { setCustomKey(any(), any<String>()) } returns Unit
        },
        analytics = mockk {
            every { logEvent(any(), any()) } returns Unit
        }
    )