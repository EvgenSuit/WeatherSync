package com.weathersync.features.settings.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.R
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.presentation.ui.SettingsScreen
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.hamcrest.core.AllOf.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPrivacyTermsUITest {
    @get: Rule
    val composeRule = createComposeRule()
    @get: Rule
    val settingsBaseRule = SettingsBaseRule()
    private val snackbarScope = TestScope()

    @Test
    fun collectDefaultTheme_isCorrect() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                SettingsScreen(viewModel = settingsBaseRule.viewModel, onSignOut = {})
            }) {
            performLinkCheck(privacyPolicy = true)
            performLinkCheck(privacyPolicy = false)
        }
    }

    private fun ComposeContentTestRule.performLinkCheck(privacyPolicy: Boolean) {
        val text = getString(if (privacyPolicy) R.string.privacy_policy else R.string.terms_of_service)
        val link = getString(if (privacyPolicy) R.string.privacy_policy_link else R.string.terms_of_service_link)
        Intents.init()
        onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        Intents.intended(
            allOf(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasData(Uri.parse(link)))
        )

        Intents.release()
    }
}