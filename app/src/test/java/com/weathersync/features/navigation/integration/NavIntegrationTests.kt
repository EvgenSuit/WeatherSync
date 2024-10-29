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


    @Test
    fun loadUser_isUICorrect() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = baseNavRule.testHelper.snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            launch {
                baseNavIntegrationRule.loadUser(
                    composeRule = composeRule,
                    isUserNullFlow = baseNavRule.viewModel.isUserNullFlow
                )
            }
        }
    }

    @Test
    fun navigateToScreens_isUICorrect() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = baseNavRule.testHelper.snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            launch {
                baseNavIntegrationRule.loadUser(
                    composeRule = composeRule,
                    isUserNullFlow = baseNavRule.viewModel.isUserNullFlow
                )
                baseNavIntegrationRule.navigateToRoute(composeRule = composeRule, *topLevelRoutes.toTypedArray())
                assertEquals(topLevelRoutes.size, baseNavIntegrationRule.navController.backStack.size)
            }
        }
    }

    @Test
    fun navigateToSameScreen_backStackIsSame() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = baseNavRule.testHelper.snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            launch {
                baseNavIntegrationRule.loadUser(
                    composeRule = composeRule,
                    isUserNullFlow = baseNavRule.viewModel.isUserNullFlow
                )
                val sizeBefore = baseNavIntegrationRule.navController.backStack.size
                baseNavIntegrationRule.navigateToRoute(composeRule = composeRule, Route.Home)
                val sizeAfter = baseNavIntegrationRule.navController.backStack.size
                assertEquals(sizeAfter, sizeBefore)
            }
        }
    }
    @Test
    fun pressBackOutsideOfHome_isInHome() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = baseNavRule.testHelper.snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            launch {
                baseNavIntegrationRule.loadUser(
                    composeRule = composeRule,
                    isUserNullFlow = baseNavRule.viewModel.isUserNullFlow
                )
                baseNavIntegrationRule.navigateToRoute(composeRule = composeRule, Route.ActivityPlanning)
                baseNavIntegrationRule.navController.popBackStack()
                baseNavIntegrationRule.assertRouteEquals(Route.Home)
            }
        }
    }
    @Test
    fun pressBackInHome_isRouteNull() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = baseNavRule.testHelper.snackbarScope,
            uiContent = {
                NavManager(navController = baseNavIntegrationRule.navController, navManagerViewModel = baseNavRule.viewModel)
            }) {
            launch {
                baseNavIntegrationRule.loadUser(
                    composeRule = composeRule,
                    isUserNullFlow = baseNavRule.viewModel.isUserNullFlow
                )
                baseNavIntegrationRule.navController.popBackStack()
                baseNavIntegrationRule.assertRouteEquals(null)
            }
        }
    }

}