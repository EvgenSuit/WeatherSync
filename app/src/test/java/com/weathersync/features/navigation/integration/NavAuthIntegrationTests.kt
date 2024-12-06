package com.weathersync.features.navigation.integration

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.R
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.validEmail
import com.weathersync.common.auth.validPassword
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.features.navigation.BaseNavRule
import com.weathersync.features.navigation.presentation.ui.NavManager
import com.weathersync.features.navigation.presentation.ui.Route
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
class NavAuthIntegrationTests {
    @get: Rule
    val composeRule = createComposeRule()

    @get: Rule
    val baseNavRule = BaseNavRule()
    @get: Rule
    val baseNavIntegrationRule = BaseNavIntegrationRule()
    private val snackbarScope = TestScope()

    @Test
    fun signOut_isInAuth() {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.assertRouteEquals(Route.Home)
            signOut()
        }
    }

    @Test
    fun signIn_userIsNull_isInHome() {
        baseNavRule.apply {
            stopKoin()
            setupKoin(inputAuth = mockAuth(user = null))
            setupViewModel()
        }
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.assertRouteEquals(Route.Auth)
            signIn()
            assertEquals(2, baseNavIntegrationRule.navController.backStack.size)
        }
    }

    @Test
    fun signOutTwice_isInAuth() {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.assertRouteEquals(Route.Home)
            signOut()
            signIn()
            signOut()
        }
    }


    private fun ComposeContentTestRule.signOut() {
        baseNavIntegrationRule.navigateToRoute(composeRule = composeRule, Route.Settings)
        onNodeWithText(getString(R.string.sign_out)).performClick()
        // wait for SignOut event to be emitted in the SettingsScreen LaunchedEffect
        waitForIdle()
        baseNavIntegrationRule.assertRouteEquals(Route.Auth)

        // graph and the auth destination itself
        assertEquals(2, baseNavIntegrationRule.navController.backStack.size)
    }
    private fun ComposeContentTestRule.signIn() {
        onNodeWithTag(getString(R.string.email)).performTextReplacement(validEmail)
        onNodeWithTag(getString(R.string.password)).performTextReplacement(validPassword)
        onNodeWithText(getString(R.string.sign_in)).performClick()

        waitForIdle()
        baseNavIntegrationRule.assertRouteEquals(Route.Home)
        assertEquals(2, baseNavIntegrationRule.navController.backStack.size)
    }
}