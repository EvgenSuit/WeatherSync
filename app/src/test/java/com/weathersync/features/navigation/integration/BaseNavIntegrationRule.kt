package com.weathersync.features.navigation.integration

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.weathersync.features.navigation.presentation.ui.Route
import org.junit.Assert.assertEquals
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class BaseNavIntegrationRule: TestWatcher() {
    lateinit var navController: TestNavHostController

    override fun starting(description: Description?) {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    fun navigateToRoute(
        composeRule: ComposeContentTestRule,
        vararg route: Route
    ) {
        composeRule.apply {
            route.forEach {
                onNodeWithContentDescription(it.route).performClick()
                assertRouteEquals(it)
            }
        }
    }
    fun assertRouteEquals(route: Route?) =
        assertEquals(route?.route, navController.currentBackStackEntry?.destination?.route)
}