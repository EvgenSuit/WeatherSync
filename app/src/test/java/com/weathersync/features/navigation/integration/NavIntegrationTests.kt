package com.weathersync.features.navigation.integration

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.features.navigation.BaseNavRule
import com.weathersync.features.navigation.presentation.ui.NavManager
import com.weathersync.features.navigation.presentation.ui.Route
import com.weathersync.features.navigation.presentation.ui.topLevelRoutes
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavIntegrationTests {
    @get: Rule
    val composeRule = createComposeRule()

    @get: Rule
    val baseNavRule = BaseNavRule()
    @get: Rule
    val baseNavIntegrationRule = BaseNavIntegrationRule()
    private val snackbarScope = TestScope()

    @Test
    fun loadUser_isUICorrect() {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.assertRouteEquals(Route.Home)
        }
    }

    @Test
    fun navigateToTopLevelScreens_isBackStackCorrect() {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(
                    activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.apply {
                assertRouteEquals(Route.Home)
                navigateToRoute(composeRule = composeRule, *topLevelRoutes.toTypedArray())
                navigateToRoute(composeRule = composeRule, *topLevelRoutes.toTypedArray())
                // 2 since back stack contains of graph and current route
                assertEquals(2, navController.backStack.size)
            }
        }
    }
    @Test
    fun navigateToSubscription_clickOnBackButton() = runBlocking {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(
                    activity = mockk(),
                    navController = baseNavIntegrationRule.navController,
                    navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.apply {
                assertRouteEquals(Route.Home)
                launch {
                    baseNavRule.subscriptionInfoDatastore.setIsSubscribed(false)
                    waitForIdle()
                    onNodeWithContentDescription(Route.Premium.icon!!.name,
                        useUnmergedTree = true).assertIsDisplayed().performClick()
                    assertRouteEquals(Route.Premium)
                    assertEquals(3, navController.backStack.size)

                    onNodeWithContentDescription(Icons.AutoMirrored.Default.ArrowBack.name).performClick()
                    waitForIdle()
                    assertRouteEquals(Route.Home)

                    // 2 since back stack contains of graph and current route
                    assertEquals(2, navController.backStack.size)
                }
            }
        }
    }
    @Test
    fun navigateToSubscription_isSubscribedNavigatedBack() = runBlocking {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(
                    activity = mockk(),
                    navController = baseNavIntegrationRule.navController,
                    navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.apply {
                assertRouteEquals(Route.Home)
                launch {
                    baseNavRule.subscriptionInfoDatastore.setIsSubscribed(false)
                    waitForIdle()

                    onNodeWithContentDescription(Route.Premium.icon!!.name,
                        useUnmergedTree = true).performClick()
                    assertRouteEquals(Route.Premium)
                    assertEquals(3, navController.backStack.size)

                    baseNavRule.subscriptionInfoDatastore.setIsSubscribed(true)
                    repeat(1000) {
                        waitForIdle()
                    }
                    assertRouteEquals(Route.Home)

                    // 2 since back stack contains graph and current route
                    assertEquals(2, navController.backStack.size)
                }
            }
        }
    }

    @Test
    fun navigateToSameScreen_backStackIsSame() {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.apply {
                assertRouteEquals(Route.Home)
                val sizeBefore = navController.backStack.size
                navigateToRoute(composeRule = composeRule, Route.Home)
                val sizeAfter = navController.backStack.size
                assertEquals(sizeAfter, sizeBefore)
            }
        }
    }
    @Test
    fun pressBackOutsideOfHome_isInOutsideOfApp() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.apply {
                assertRouteEquals(Route.Home)
                navigateToRoute(composeRule = composeRule, Route.ActivityPlanning)
                navController.popBackStack()
                assertRouteEquals(null)
            }
        }
    }
    @Test
    fun pressBackInHome_isInOutsideOfApp() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(activity = mockk(),
                    navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.apply {
                assertRouteEquals(Route.Home)
                navController.popBackStack()
                assertRouteEquals(null)
            }
        }
    }

}