package com.weathersync.features.navigation.integration

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.R
import com.weathersync.common.auth.validEmail
import com.weathersync.common.auth.validPassword
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.navigation.BaseNavRule
import com.weathersync.features.navigation.presentation.ui.NavManager
import com.weathersync.features.navigation.presentation.ui.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class NavAuthIntegrationTests {
    @get: Rule
    val composeRule = createComposeRule()

    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val baseNavRule = BaseNavRule()
    @get: Rule(order = 2)
    val baseNavIntegrationRule = BaseNavIntegrationRule()
    private val snackbarScope = TestScope()

    @Test
    fun signOut_isInAuth() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            launch {
                baseNavIntegrationRule.loadUser(
                    composeRule = composeRule,
                    isUserNullFlow = baseNavRule.viewModel.isUserNullFlow
                )
                signOut(testScope = this@runTest)
            }
        }
    }

    @Test
    fun signIn_isInHome() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            launch {
                baseNavIntegrationRule.loadUser(
                    composeRule = composeRule,
                    isUserNullFlow = baseNavRule.viewModel.isUserNullFlow
                )
                signOut(testScope = this@runTest)
                signIn(testScope = this@runTest)
            }
        }
    }

    @Test
    fun signOutTwice_isInAuth() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            launch {
                baseNavIntegrationRule.loadUser(
                    composeRule = composeRule,
                    isUserNullFlow = baseNavRule.viewModel.isUserNullFlow
                )
                signOut(testScope = this@runTest)
                signIn(testScope = this@runTest)
                signOut(testScope = this@runTest)
            }
        }
    }


    private fun ComposeContentTestRule.signOut(testScope: TestScope) {
        baseNavIntegrationRule.navigateToRoute(composeRule = composeRule, Route.Settings)
        onNodeWithText(getString(R.string.sign_out)).performClick()
        // advance viewModelScope in isUserNullFlow's stateIn method
        testScope.advanceUntilIdle()
        // wait for UI to receive updates from the isUserNull flow
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
        baseNavIntegrationRule.assertRouteEquals(Route.Home)
        assertEquals(2, baseNavIntegrationRule.navController.backStack.size)
    }
}