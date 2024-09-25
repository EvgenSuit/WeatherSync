package com.weathersync.common

import com.weathersync.common.auth.mockAuth
import com.weathersync.common.utils.mockCrashlyticsManager
import com.weathersync.utils.CoroutineScopeProvider
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.TestScope
import org.junit.Before

open class BaseTest {
    val testScope = TestScope()
    val coroutineScopeProvider = CoroutineScopeProvider(testScope)
    val auth = mockAuth()
    val crashlyticsExceptionSlot = slot<Exception>()
    val crashlyticsManager = mockCrashlyticsManager(exceptionSlot = crashlyticsExceptionSlot)

    @Before
    fun beforeTest() {
        unmockkAll()
    }
}