package com.weathersync.common

import com.weathersync.common.auth.mockAuth
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.common.utils.mockCrashlyticsManager
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Rule
import org.koin.core.context.stopKoin

open class BaseTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()
    val snackbarScope = TestScope()

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