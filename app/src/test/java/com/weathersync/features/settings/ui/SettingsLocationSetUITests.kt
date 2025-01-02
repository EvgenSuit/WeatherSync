package com.weathersync.features.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.presentation.ui.SettingsScreen
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.weathersync.R
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.weather.LocationInfo
import com.weathersync.common.weather.fullLocation
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import com.weathersync.utils.weather.limits.QueryType
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class SettingsLocationSetUITests {
    @get: Rule(order = 1)
    val settingsBaseRule = SettingsBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(settingsBaseRule.testDispatcher)
    @get: Rule
    val composeRule = createComposeRule()
    private val snackbarScope = TestScope()

    @Test
    fun notSubscribed_customLocationSetNotVisible_setCurrentLocationAsDefault() = runTest {
        settingsBaseRule.apply {
            setContentWithSnackbar(composeRule = composeRule,
                snackbarScope = snackbarScope,
                uiContent = {
                    SettingsScreen(viewModel = viewModel, onSignOut = {}) }) {
                onNodeWithText(getString(R.string.set_location)).performClick()
                // advance view model scope and LaunchedEffect
                advanceUntilIdle()
                onNodeWithTag("SetLocationSheet").assertIsDisplayed()
                onNodeWithTag("SetCustomLocationTextField").assertDoesNotExist()
                onNodeWithTag("SetCustomLocationButton").assertDoesNotExist()

                onNodeWithText(getString(R.string.set_current_location_as_default)).assertIsDisplayed().assertIsEnabled().performClick()
                testHelper.advance(this@runTest)
                onNodeWithTag("SetLocationSheet").assertIsNotDisplayed()
                assertSnackbarTextEquals(R.string.location_set_successfully, snackbarScope,
                    LocationInfo().fullLocation())
            }
        }
    }
    @Test
    fun subscribed_customLocationSetVisible_setCustomLocation() = runTest {
        settingsBaseRule.apply {
            setupRepository(isSubscribed = true)
            setupViewModel()
            advanceUntilIdle()

            subscriptionInfoDatastore.setIsSubscribed(true)
            setContentWithSnackbar(composeRule = composeRule,
                snackbarScope = snackbarScope,
                uiContent = {
                    SettingsScreen(viewModel = viewModel, onSignOut = {}) }) {
                // since i'm using SharingStarted.WhileSubscribed() for isSubscribed in the view model,
                // it's necessary to advance viewModelScope ONLY when the UI content has already been set
                advanceUntilIdle()
                performCustomLocationSet()

                testHelper.advance(this@runTest)
                onNodeWithTag("SetLocationSheet").assertIsNotDisplayed()
                assertSnackbarTextEquals(R.string.location_set_successfully, snackbarScope,
                    LocationInfo().fullLocation())
            }
        }
    }
    @Test
    fun subscribed_customLocationSetVisible_setCustomLocation_limitReached() = runTest {
        settingsBaseRule.apply {
            setupLimitManager(timestamps = createDescendingTimestamps(
                limitManagerConfig = QueryType.LocationSet.premiumLimitManagerConfig,
                currTimeMillis = testHelper.testClock.millis()
            ))
            setupRepository(isSubscribed = true)
            setupViewModel()
            advanceUntilIdle()

            subscriptionInfoDatastore.setIsSubscribed(true)
            setContentWithSnackbar(composeRule = composeRule,
                snackbarScope = snackbarScope,
                uiContent = {
                    SettingsScreen(viewModel = viewModel, onSignOut = {}) }) {
                advanceUntilIdle()

                launch {
                    performCustomLocationSet()

                    val limit = settingsRepository.calculateLocationSetLimits(true)
                    val formattedLimit = NextUpdateTimeFormatter(clock = testHelper.testClock, locale = Locale.US)
                        .format(limit.nextUpdateDateTime!!)
                    onNodeWithText(getString(R.string.next_location_set_time, formattedLimit)).assertIsDisplayed()
                    onNodeWithTag("SetLocationSheet").assertIsDisplayed()
                }
            }
        }
    }
    @Test
    fun subscribed_customLocationSetVisible_setCustomLocation_error() = runTest {
        settingsBaseRule.apply {
            setupRepository(isSubscribed = true, locationManagerException = testHelper.testException)
            setupViewModel()
            advanceUntilIdle()

            subscriptionInfoDatastore.setIsSubscribed(true)
            setContentWithSnackbar(composeRule = composeRule,
                snackbarScope = snackbarScope,
                uiContent = {
                    SettingsScreen(viewModel = viewModel, onSignOut = {}) }) {
                advanceUntilIdle()
                performCustomLocationSet()

                testHelper.advance(this@runTest)
                onNodeWithTag("SetLocationSheet").assertIsNotDisplayed()
                assertSnackbarTextEquals(R.string.could_not_set_location, snackbarScope)
            }
        }
    }

    private fun TestScope.performCustomLocationSet() {
        composeRule.apply {
            onNodeWithText(getString(R.string.set_location)).performClick()
            // advance view model scope
            advanceUntilIdle()
            onNodeWithTag("SetLocationSheet").assertIsDisplayed()
            val setCustomLocationButton = onNodeWithTag("SetCustomLocationButton")
            setCustomLocationButton.assertIsDisplayed().assertIsNotEnabled()
            onNodeWithTag("SetCustomLocationTextField").assertIsDisplayed().performTextInput(LocationInfo().fullLocation())
            onNodeWithText(getString(R.string.set_current_location_as_default)).assertIsDisplayed().assertIsEnabled()
            onNodeWithTag("SetCustomLocationButton").assertIsDisplayed().assertIsEnabled().performClick()

            settingsBaseRule.testHelper.advance(this@performCustomLocationSet)
        }
    }
}