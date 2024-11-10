package com.weathersync.features.settings.ui

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.WeatherUnitTest
import com.weathersync.features.settings.presentation.ui.SettingsScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.weathersync.R
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.features.settings.data.WeatherUnit
import kotlinx.coroutines.test.TestScope

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsWeatherUnitsUITests: WeatherUnitTest {
    @get: Rule
    val composeRule = createComposeRule()
    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val settingsBaseRule = SettingsBaseRule()
    private val snackbarScope = TestScope()

    @Test
    override fun fetchUnits_success() = runTest {
        fetchUnits(success = true, testScope = this)
    }

    @Test
    override fun fetchUnits_exception() = runTest {
        settingsBaseRule.apply {
            setupWeatherUnitsManager(unitsFetchException = testHelper.testException)
            setupRepository()
            setupViewModel()
        }
        fetchUnits(success = false, testScope = this)
    }

    @Test
    override fun setUnits_success() = runTest {
        setUnit(success = true, testScope = this)
    }

    @Test
    override fun setUnits_exception() = runTest {
        settingsBaseRule.apply {
            setupWeatherUnitsManager(unitSetException = testHelper.testException)
            setupRepository()
            setupViewModel()
        }
        setUnit(success = false, testScope = this)
    }

    private fun fetchUnits(success: Boolean, testScope: TestScope) {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                SettingsScreen(viewModel = settingsBaseRule.viewModel, onSignOut = {})
            }) {
            // MUST call inside of testContent body of setContentWithSnackbar, otherwise the ui event doesn't get emitted during fetch
            testScope.advanceUntilIdle()
            fetchedWeatherUnits.forEach { unit ->
                onNodeWithText(unit.unitName, useUnmergedTree = true).apply { if (success) assertIsDisplayed() else assertIsNotDisplayed() }
            }
            onAllNodesWithTag("Dropdown").assertAll(isEnabled())
            if (!success) assertSnackbarTextEquals(resId = R.string.could_not_load_units, snackbarScope)
            else assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }

    private fun setUnit(success: Boolean, testScope: TestScope) {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                SettingsScreen(viewModel = settingsBaseRule.viewModel, onSignOut = {})
            }) {
            testScope.advanceUntilIdle()
            fetchedWeatherUnits.forEach { unit ->
                onNodeWithText(unit.unitName, useUnmergedTree = true).assertIsDisplayed()
            }
            val unitToChange = onNodeWithText(fetchedWeatherUnits[0].unitName, useUnmergedTree = true)
            val unitToSelect = onNodeWithText(WeatherUnit.Temperature.Fahrenheit.unitName, useUnmergedTree = true)
            unitToChange.performClick()
            unitToSelect.assertIsDisplayed().performClick()

            unitToChange.assertIsDisplayed().assertIsNotEnabled()
            onNodeWithTag("Loading").assertIsDisplayed()
            onAllNodesWithTag("Dropdown").assertAll(isNotEnabled())

            testScope.advanceUntilIdle()
            unitToChange.apply { if (success) assertIsNotDisplayed() else assertIsDisplayed() }
            unitToSelect.apply { if (success) assertIsDisplayed() else assertIsNotDisplayed() }
            if (!success) assertSnackbarTextEquals(resId = R.string.could_not_set_units, snackbarScope)
            else assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }

}