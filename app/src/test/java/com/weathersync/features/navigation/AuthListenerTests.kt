package com.weathersync.features.navigation

import app.cash.turbine.test
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthListenerTests {
    @get: Rule
    val baseNavRule = BaseNavRule()

    @Test
    fun signOut_userIsNull() = runTest {
        baseNavRule.authListener.isUserNullFlow().test {
            assertFalse(awaitItem())
            baseNavRule.auth.signOut()
            assertTrue(awaitItem())
        }
    }
}