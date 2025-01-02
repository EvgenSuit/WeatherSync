package com.weathersync.features.settings.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.MainDispatcherRule
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.ThemeTest
import com.weathersync.features.settings.presentation.ui.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsThemeUITests: ThemeTest {
    @get: Rule
    val composeRule = createComposeRule()

    @get: Rule(order = 1)
    val settingsBaseRule = SettingsBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(settingsBaseRule.testDispatcher)
    private val snackbarScope = TestScope()

    @Test
    override fun collectDefaultTheme_isCorrect() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                SettingsScreen(viewModel = settingsBaseRule.viewModel, onSignOut = {})
            }) {
            advanceUntilIdle()
            onNodeWithTag("ThemeSwitcher").apply {
                launch {
                    assertIsNotEnabled()
                    settingsBaseRule.viewModel.themeState.test {
                        skipItems(1) // skip null
                        awaitItem()
                        onNodeWithContentDescription("DarkMode", useUnmergedTree = true).assertIsSelected()
                        onNodeWithContentDescription("LightMode", useUnmergedTree = true).assertIsNotSelected()
                    }
                    assertIsEnabled()
                }
            }
        }
    }

    @Test
    override fun setTheme_isCorrect() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                SettingsScreen(viewModel = settingsBaseRule.viewModel, onSignOut = {})
            }) {
            advanceUntilIdle()
            onNodeWithTag("ThemeSwitcher").apply {
                launch {
                    assertIsNotEnabled()
                    settingsBaseRule.viewModel.themeState.test {
                        skipItems(1)
                        assertEquals(true, awaitItem())
                        assertIsEnabled()

                        // order of "dark" values doesn't matter here
                        for (dark in listOf(false, true)) {
                            // set theme to light on the first run
                            performClick()
                            advanceUntilIdle()
                            awaitItem()
                            onNodeWithContentDescription("DarkMode", useUnmergedTree = true).apply {
                                if (!dark) assertIsNotSelected()
                                else assertIsSelected()
                            }
                            onNodeWithContentDescription("LightMode", useUnmergedTree = true).apply {
                                if (dark) assertIsNotSelected()
                                else assertIsSelected()
                            }
                        }
                    }
                }
            }
        }
    }
}