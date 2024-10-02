package com.weathersync.features.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.weathersync.clearFocusOnNonButtonClick
import com.weathersync.common.ui.CustomSnackbar
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.features.activityPlanning.presentation.ui.ActivityPlanningScreen
import com.weathersync.features.auth.presentation.ui.AuthScreen
import com.weathersync.features.home.presentation.ui.HomeScreen
import com.weathersync.ui.theme.WeatherSyncTheme

sealed class Route(val route: String, val icon: ImageVector? = null) {
    data object Auth: Route("Auth")
    data object Home: Route("Home", Icons.Filled.Home)
    data object ActivityPlanning: Route("ActivityPlanning", Icons.Filled.DateRange)
}

val topLevelRoutes = listOf(
    Route.Home,
    Route.ActivityPlanning
)

@Composable
fun NavManager(
    navController: NavHostController = rememberNavController()
) {
    val snackbarController = LocalSnackbarController.current
    val isUserNull = Firebase.auth.currentUser == null
    val currRoute by navController.currentBackStackEntryAsState()
    val enterAnimation = slideInVertically { it/4 }
    val exitAnimation = fadeOut()
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarController.hostState) {
                CustomSnackbar(snackbarHostState = snackbarController.hostState,
                    onDismiss = { snackbarController.hostState.currentSnackbarData?.dismiss() })
            }
        },
        bottomBar = {
            AnimatedVisibility(currRoute?.destination?.route in topLevelRoutes.map { it.route },
                enter = fadeIn(), exit = fadeOut()
            ) {
                BottomBar(currRoute = currRoute?.destination?.route ?: Route.Auth.route,
                    onNavigateToRoute = { navController.navigate(it.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    } })
            }
        }
    ){ innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isUserNull) Route.Auth.route else Route.Home.route,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(WindowInsets.ime)
                .clearFocusOnNonButtonClick(LocalFocusManager.current)
        ) {
            composable(Route.Auth.route) {
                AuthScreen(onNavigateToHome = { navController.navigate(Route.Home.route) {
                    popUpTo(Route.Auth.route) {
                        inclusive = true
                    }
                } }) }
            composable(Route.Home.route,
                enterTransition = { enterAnimation },
                exitTransition = { exitAnimation }) {
                HomeScreen()
            }
            composable(Route.ActivityPlanning.route,
                enterTransition = { enterAnimation },
                exitTransition = { exitAnimation }) {
                ActivityPlanningScreen()
            }
        }
    }
}

@Composable
fun BottomBar(
    currRoute: String,
    onNavigateToRoute: (Route) -> Unit
) {
    NavigationBar(
        modifier = Modifier.padding(0.dp)
    ) {
        topLevelRoutes.forEach { route ->
            NavigationBarItem(
                selected = currRoute == route.route,
                onClick = { onNavigateToRoute(route) },
                icon = { route.icon?.let { icon ->
                    Icon(imageVector = icon, contentDescription = route.route)
                } })
        }
    }
}

@Preview
@Composable
fun ButtomBarPreview() {
    WeatherSyncTheme {
        Surface {
            BottomBar(currRoute = Route.Home.route) {
                
            }
        }
    }
}