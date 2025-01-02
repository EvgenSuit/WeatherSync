package com.weathersync.features.settings.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.weather.LocationInfo
import com.weathersync.common.weather.fullLocation
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.weather.limits.QueryType
import io.mockk.coVerify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryLocationSetTests {
    @get: Rule(order = 1)
    val settingsBaseRule = SettingsBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(settingsBaseRule.testDispatcher)


    @Test
    fun calculateLocationSetLimits_isSubscribed() = runTest {
        settingsBaseRule.apply {
            settingsRepository.calculateLocationSetLimits(isSubscribed = true)
            coVerify(exactly = 1) { limitManager.calculateLimit(isSubscribed = true, queryType = QueryType.LocationSet) }
        }
    }
    @Test
    fun calculateLocationSetLimits_limitNotReached() = runTest {
        calculateLocationSetLimitsUtil(isLimitReached = false)
    }
    @Test
    fun calculateLocationSetLimits_limitReached() = runTest {
        calculateLocationSetLimitsUtil(isLimitReached = true)
    }

    // account for only when is subscribed, since limit calculation happens only if a user has subscription
    private suspend fun calculateLocationSetLimitsUtil(isLimitReached: Boolean) {
        settingsBaseRule.apply {
            if (isLimitReached) {
                setupLimitManager(timestamps = createDescendingTimestamps(
                    limitManagerConfig = QueryType.LocationSet.premiumLimitManagerConfig,
                    currTimeMillis = testHelper.testClock.millis()
                ))
                setupRepository()
            }
            settingsRepository.calculateLocationSetLimits(isSubscribed = true).let {
                 if (isLimitReached) assertTrue(it.isReached) else assertFalse(it.isReached)
            }
            coVerify(exactly = 1) { limitManager.calculateLimit(isSubscribed = true, queryType = QueryType.LocationSet) }
        }
    }
    @Test
    fun incrementLocationLimit() = runTest {
        settingsBaseRule.apply {
            settingsRepository.incrementLocationSetLimits()
            coVerify(exactly = 1) { limitManager.recordTimestamp(queryType = QueryType.LocationSet) }
        }
    }
    @Test
    fun setLocation() = runTest {
        settingsBaseRule.apply {
            val inputLocation = LocationInfo().fullLocation()
            assertEquals(inputLocation, settingsRepository.setLocation(inputLocation))
            coVerify(exactly = 1) { locationManager.setLocation(inputLocation) }
            coVerify(inverse = true) { locationManager.setCurrLocationAsDefault() }
        }
    }
    @Test
    fun setCurrLocationAsDefault() = runTest {
        settingsBaseRule.apply {
            assertEquals(LocationInfo().fullLocation(), settingsRepository.setCurrLocationAsDefault())
            coVerify(exactly = 1) { locationManager.setCurrLocationAsDefault() }
            coVerify(inverse = true) { locationManager.setLocation(any()) }
        }
    }
}