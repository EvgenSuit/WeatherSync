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
import com.weathersync.common.MainDispatcherRule
import com.weathersync.features.navigation.BaseNavRule
import com.weathersync.features.navigation.presentation.ui.NavManager
import com.weathersync.features.navigation.presentation.ui.Route
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class NavAuthIntegrationTests {
    @get: Rule
    val composeRule = createComposeRule()

    @get: Rule(order = 1)
    val baseNavRule = BaseNavRule()
    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule(baseNavRule.testDispatcher)
    @get: Rule(order = 2)
    val baseNavIntegrationRule = BaseNavIntegrationRule()
    private val snackbarScope = TestScope()

    @Test
    fun signOut_isInAuth() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.assertRouteEquals(Route.Home)
            signOut(testScope = this@runTest)
        }
    }

    @Test
    fun signIn_userIsNull_isInHome() = runTest {
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
            signIn(testScope = this@runTest)
            assertEquals(2, baseNavIntegrationRule.navController.backStack.size)
        }
    }

    @Test
    fun signOutTwice_isInAuth() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.assertRouteEquals(Route.Home)
            signOut(testScope = this@runTest)
            signIn(testScope = this@runTest)
            signOut(testScope = this@runTest)
        }
    }


    private fun ComposeContentTestRule.signOut(testScope: TestScope) {
        baseNavIntegrationRule.navigateToRoute(composeRule = composeRule, Route.Settings)
        onNodeWithText(getString(R.string.sign_out)).performClick()
        // advance event emission in the signOut method of SettingsViewModel
        testScope.advanceUntilIdle()
        // wait for SignOut event to be emitted in the SettingsScreen LaunchedEffect
        waitForIdle()
        baseNavIntegrationRule.assertRouteEquals(Route.Auth)

        // graph and the auth destination itself
        assertEquals(2, baseNavIntegrationRule.navController.backStack.size)
    }
    private fun ComposeContentTestRule.signIn(testScope: TestScope) {
        onNodeWithTag(getString(R.string.email)).performTextReplacement(validEmail)
        onNodeWithTag(getString(R.string.password)).performTextReplacement(validPassword)
        onNodeWithText(getString(R.string.sign_in)).performClick()
        testScope.advanceUntilIdle()

        waitForIdle()
        assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)
        baseNavIntegrationRule.assertRouteEquals(Route.Home)
        assertEquals(2, baseNavIntegrationRule.navController.backStack.size)
    }
}