package com.weathersync.features.settings.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.presentation.ui.SettingsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.weathersync.R
import com.weathersync.ui.SettingsUIEvent
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class SettingsAuthUITests {
    @get: Rule
    val composeRule = createComposeRule()

    @get: Rule
    val settingsBaseRule = SettingsBaseRule()
    private val snackbarScope = TestScope()

    @Test
    fun signOut_isUserNull() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                SettingsScreen(viewModel = settingsBaseRule.viewModel, onSignOut = {})
            }) {
            launch {
                settingsBaseRule.viewModel.uiEvents.test {
                    onNodeWithText(getString(R.string.sign_out)).performClick()
                    assertEquals(null, settingsBaseRule.auth.currentUser)
                    verify { settingsBaseRule.settingsRepository.signOut() }
                    assertTrue(awaitItem() is SettingsUIEvent.SignOut)
                }
            }
        }
    }
}