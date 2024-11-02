package com.weathersync.features.navigation.integration

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.navigation.BaseNavRule
import com.weathersync.features.navigation.presentation.ui.NavManager
import com.weathersync.features.navigation.presentation.ui.Route
import com.weathersync.features.navigation.presentation.ui.topLevelRoutes
import kotlinx.coroutines.launch
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

    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val baseNavRule = BaseNavRule()
    @get: Rule
    val baseNavIntegrationRule = BaseNavIntegrationRule()
    private val snackbarScope = TestScope()


    @Test
    fun loadUser_isUICorrect() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.assertRouteEquals(Route.Home)
        }
    }

    @Test
    fun navigateToScreens_isUICorrect() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.apply {
                assertRouteEquals(Route.Home)
                navigateToRoute(composeRule = composeRule, *topLevelRoutes.toTypedArray())
                navigateToRoute(composeRule = composeRule, *topLevelRoutes.toTypedArray())
                assertEquals(2, navController.backStack.size)
            }
        }
    }

    @Test
    fun navigateToSameScreen_backStackIsSame() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
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
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
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
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            baseNavIntegrationRule.apply {
                assertRouteEquals(Route.Home)
                navController.popBackStack()
                assertRouteEquals(null)
            }
        }
    }

}