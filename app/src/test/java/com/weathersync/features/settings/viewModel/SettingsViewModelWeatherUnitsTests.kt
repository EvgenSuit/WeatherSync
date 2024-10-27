package com.weathersync.features.settings.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.weathersync.common.TestException
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.common.utils.fetchedWeatherUnits
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.WeatherUnitTest
import com.weathersync.features.settings.data.SelectedWeatherUnits
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.features.settings.presentation.ui.SettingsIntent
import com.weathersync.utils.CustomResult
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelWeatherUnitsTests: WeatherUnitTest {
    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val settingsBaseRule = SettingsBaseRule()
    private val testException = TestException("exception")


    @Test
    override fun fetchUnits_success() = runTest {
        verifyUnitsFetch(refresh = false, success = true)
    }
    @Test
    override fun fetchUnits_exception() = runTest {
        settingsBaseRule.apply {
            setupWeatherUnitsManager(unitsFetchException = testException)
            setupRepository()
            setupViewModel()
        }
        verifyUnitsFetch(refresh = false, success = false)
    }

    @Test
    fun refreshUnits_success() = runTest {
        performUnitsRefresh(success = true)
    }
    @Test
    fun refreshUnits_exception() = runTest {
        settingsBaseRule.apply {
            setupWeatherUnitsManager(unitsFetchException = testException)
            setupRepository()
            setupViewModel()
        }
        performUnitsRefresh(success = false)
    }

    @Test
    override fun setUnits_success() = runTest {
        performUnitSet(success = true)
    }
    @Test
    override fun setUnits_exception() = runTest {
        settingsBaseRule.apply {
            setupWeatherUnitsManager(unitSetException = testException)
            setupRepository()
            setupViewModel()
        }
        performUnitSet(success = false)
    }

    private fun TestScope.performUnitsRefresh(success: Boolean) {
        settingsBaseRule.viewModel.apply {
            handleIntent(SettingsIntent.FetchWeatherUnits(refresh = true))
            assertEquals(CustomResult.InProgress, settingsBaseRule.viewModel.uiState.value.weatherUnitsRefreshResult)
            verifyUnitsFetch(refresh = true, success = success)
        }
    }
    private fun TestScope.performUnitSet(success: Boolean) {
        settingsBaseRule.viewModel.apply {
            for (unit in listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.WindSpeed.MPH, WeatherUnit.Visibility.Meters)) {
                handleIntent(SettingsIntent.SetWeatherUnit(unit))
                assertEquals(CustomResult.InProgress, uiState.value.weatherUnitSetResult)
                advanceUntilIdle()
                coVerify(exactly = 1) { settingsBaseRule.settingsRepository.setWeatherUnit(unit) }
                if (!success) {
                    assertTrue(settingsBaseRule.testHelper.crashlyticsExceptionSlot.captured is TestException)
                    assertEquals(CustomResult.Error, uiState.value.weatherUnitSetResult)
                    assertTrue(settingsBaseRule.testHelper.crashlyticsExceptionSlot.isCaptured)
                } else {
                    assertFalse(settingsBaseRule.testHelper.crashlyticsExceptionSlot.isCaptured)
                    assertEquals(CustomResult.Success, uiState.value.weatherUnitSetResult)
                }
            }
        }
    }
    private fun TestScope.verifyUnitsFetch(
        refresh: Boolean,
        success: Boolean
    ) {
        advanceUntilIdle()
        assertEquals( if (success)
            SelectedWeatherUnits(
                temp = fetchedWeatherUnits[0] as WeatherUnit.Temperature,
                windSpeed = fetchedWeatherUnits[1] as WeatherUnit.WindSpeed,
                visibility = fetchedWeatherUnits[2] as WeatherUnit.Visibility
            ) else null, settingsBaseRule.viewModel.uiState.value.weatherUnits)

        if (!success) {
            assertTrue(settingsBaseRule.testHelper.crashlyticsExceptionSlot.captured is TestException)
            assertEquals(CustomResult.Error, settingsBaseRule.viewModel.uiState.value.weatherUnitsFetchResult)
            assertTrue(settingsBaseRule.testHelper.crashlyticsExceptionSlot.isCaptured)
        } else {
            assertFalse(settingsBaseRule.testHelper.crashlyticsExceptionSlot.isCaptured)
            assertEquals(CustomResult.Success, settingsBaseRule.viewModel.uiState.value.weatherUnitsFetchResult)
        }
        coVerify(exactly = if (refresh) 2 else 1) { settingsBaseRule.settingsRepository.getUnits() }
    }
}