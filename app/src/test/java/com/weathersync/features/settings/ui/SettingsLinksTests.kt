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
class SettingsLinksTests {
    @get: Rule
    val composeRule = createComposeRule()
    @get: Rule
    val settingsBaseRule = SettingsBaseRule()
    private val snackbarScope = TestScope()
    private sealed class LinkType {
        data object PrivacyPolicy: LinkType()
        data object TermsOfService: LinkType()
        data object ManageSubscriptions: LinkType()
    }

    @Test
    fun goToLinks_isIntentCorrect() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                SettingsScreen(viewModel = settingsBaseRule.viewModel, onSignOut = {})
            }) {
            performLinkCheck(linkType = LinkType.PrivacyPolicy)
            performLinkCheck(linkType = LinkType.TermsOfService)
            performLinkCheck(linkType = LinkType.ManageSubscriptions)
        }
    }

    private fun ComposeContentTestRule.performLinkCheck(linkType: LinkType) {
        val text = getString(when (linkType) {
            is LinkType.PrivacyPolicy -> R.string.privacy_policy
            is LinkType.TermsOfService -> R.string.terms_of_service
            is LinkType.ManageSubscriptions -> R.string.manage_subscriptions
        })

        val link = getString(when (linkType) {
            is LinkType.PrivacyPolicy -> R.string.privacy_policy_link
            is LinkType.TermsOfService -> R.string.terms_of_service_link
            is LinkType.ManageSubscriptions -> R.string.manage_subscriptions_link
        })
        Intents.init()
        onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        Intents.intended(
            allOf(IntentMatchers.hasAction(Intent.ACTION_VIEW), IntentMatchers.hasData(Uri.parse(link)))
        )
        Intents.release()
    }
}