package com.weathersync.features.navigation.presentation.ui

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.weathersync.MainActivity
import com.weathersync.clearFocusOnNonButtonClick
import com.weathersync.common.ui.CustomSnackbar
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.SnackbarController
import com.weathersync.features.activityPlanning.presentation.ui.ActivityPlanningScreen
import com.weathersync.features.auth.presentation.ui.AuthScreen
import com.weathersync.features.home.presentation.ui.HomeScreen
import com.weathersync.features.settings.presentation.ui.SettingsScreen
import com.weathersync.features.subscription.presentation.ui.SubscriptionInfoScreen
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.subscription.IsSubscribed
import org.koin.androidx.compose.koinViewModel

sealed class Route(val route: String, val icon: ImageVector? = null) {
    data object Auth: Route("Auth")
    data object Home: Route("Home", Icons.Filled.Home)
    data object ActivityPlanning: Route("ActivityPlanning", Icons.Filled.DateRange)
    data object Settings: Route("Settings", Icons.Filled.Settings)
    data object Premium: Route("Premium", Icons.Filled.WorkspacePremium)
}

val topLevelRoutes = listOf(
    Route.Home,
    Route.ActivityPlanning,
    Route.Settings
)

@Composable
fun NavManager(
    navController: NavHostController = rememberNavController(),
    activity: MainActivity,
    navManagerViewModel: NavManagerViewModel = koinViewModel()
) {
    val snackbarController = LocalSnackbarController.current
    val isSubscribed by navManagerViewModel.isUserSubscribedFlow.collectAsStateWithLifecycle()
    NavManagerContent(
        activity = activity,
        navController = navController,
        isUserNullInit = navManagerViewModel.isUserNullInit,
        isSubscribed = isSubscribed,
        snackbarController = snackbarController
    )
}

@Composable
fun NavManagerContent(
    activity: MainActivity,
    navController: NavHostController,
    isUserNullInit: Boolean,
    isSubscribed: IsSubscribed?,
    snackbarController: SnackbarController
) {
    val enterAnimation = slideInVertically { it/4 }
    val exitAnimation = fadeOut()
    val currRoute by navController.currentBackStackEntryAsState()
    val showPremiumActionButton = isSubscribed != null && !isSubscribed
            && !listOf(Route.Auth.route, Route.Premium.route).contains(currRoute?.destination?.route)
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarController.hostState) {
                CustomSnackbar(snackbarHostState = snackbarController.hostState,
                    onDismiss = { snackbarController.hostState.currentSnackbarData?.dismiss() })
            }
        },
        floatingActionButton = {
            if (showPremiumActionButton) {
                FloatingActionButton(
                    contentColor = Color.Yellow,
                    containerColor = Color.Yellow.copy(0.5f),
                    onClick = { navController.navigate(Route.Premium.route) }) {
                    val icon = Route.Premium.icon!!
                    Icon(
                        imageVector = icon,
                        contentDescription = icon.name)
                }
            }
        },
        bottomBar = {
            if(currRoute?.destination?.route in topLevelRoutes.map { it.route }) {
                BottomBar(currRoute = currRoute?.destination?.route ?: Route.Auth.route,
                    onNavigateToRoute = { navController.navigate(it.route) {
                        popUpTo(navController.graph.id) {
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
                startDestination = if (isUserNullInit) Route.Auth.route else Route.Home.route,
                enterTransition = { enterAnimation },
                exitTransition = { exitAnimation },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .windowInsetsPadding(WindowInsets.ime)
                    .clearFocusOnNonButtonClick(LocalFocusManager.current)
            ) {
                composable(Route.Auth.route) {
                    AuthScreen(onNavigateToHome = {
                        navController.navigate(Route.Home.route) {
                            popUpTo(navController.graph.id)
                        }
                    }) }
                composable(Route.Home.route) {
                    HomeScreen()
                }
                composable(Route.ActivityPlanning.route) {
                    ActivityPlanningScreen()
                }
                composable(Route.Settings.route) {
                    SettingsScreen(
                        onSignOut = {
                            navController.navigate(Route.Auth.route) {
                                popUpTo(navController.graph.id)
                            }
                        }
                    )
                }
                composable(Route.Premium.route) {
                    SubscriptionInfoScreen(
                        activity = activity,
                        onBackClick = { navController.navigateUp() }
                    )
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
fun BottomBarPreview() {
    WeatherSyncTheme {
        Surface {
            BottomBar(currRoute = Route.Home.route) {
                
            }
        }
    }
}