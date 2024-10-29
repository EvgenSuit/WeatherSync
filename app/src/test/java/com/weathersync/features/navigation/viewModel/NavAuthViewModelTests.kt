package com.weathersync.features.navigation.viewModel

import app.cash.turbine.test
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.navigation.BaseNavRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavAuthViewModelTests {
    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val baseNavRule = BaseNavRule()

    @Test
    fun signOut_userIsNull() = runTest {
        baseNavRule.viewModel.isUserNullFlow.test {
            assertEquals(null, awaitItem())
            assertFalse(awaitItem()!!)
            baseNavRule.auth.signOut()
            assertTrue(awaitItem()!!)
        }
    }
}