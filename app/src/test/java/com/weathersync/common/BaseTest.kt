package com.weathersync.common

import com.weathersync.common.auth.mockAuth
import com.weathersync.common.utils.mockCrashlyticsManager
import com.weathersync.utils.CoroutineScopeProvider
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.koin.core.context.stopKoin

open class BaseTest {
    val dispatcher = StandardTestDispatcher()
    val testScope = TestScope(dispatcher)
    val snackbarScope = TestScope()

    val coroutineScopeProvider = CoroutineScopeProvider(testScope, dispatcher = dispatcher)
    var auth = mockAuth()

    val exception = Exception("exception")
    val crashlyticsExceptionSlot = slot<Exception>()
    val crashlyticsManager = mockCrashlyticsManager(exceptionSlot = crashlyticsExceptionSlot)

    @Before
    fun beforeTest() {
        stopKoin()
        unmockkAll()
    }
}