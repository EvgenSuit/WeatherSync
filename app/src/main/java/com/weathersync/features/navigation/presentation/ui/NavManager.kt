package com.weathersync.features.navigation.presentation.ui

import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

val defaultTopLevelRoutes = listOf(
    Route.Home,
    Route.ActivityPlanning,
    Route.Settings,
    Route.Premium
)

@Composable
fun NavManager(
    navController: NavHostController = rememberNavController(),
    activity: MainActivity,
    navManagerViewModel: NavManagerViewModel = koinViewModel()
) {
    val snackbarController = LocalSnackbarController.current
    val isSubscribed by navManagerViewModel.isUserSubscribedFlow.collectAsState()
    val isThemeDark by navManagerViewModel.isThemeDark.collectAsState()
    NavManagerContent(
        activity = activity,
        navController = navController,
        isUserNullInit = navManagerViewModel.isUserNullInit,
        isSubscribed = isSubscribed,
        isThemeDark = isThemeDark,
        snackbarController = snackbarController
    )
}

@Composable
fun NavManagerContent(
    activity: MainActivity,
    navController: NavHostController,
    isUserNullInit: Boolean,
    isSubscribed: IsSubscribed?,
    isThemeDark: Boolean,
    snackbarController: SnackbarController
) {
    val enterAnimation = slideInVertically { it/4 }
    val exitAnimation = fadeOut()
    val currRoute by navController.currentBackStackEntryAsState()
    val showPremiumActionButton by remember(isSubscribed, currRoute) {
        mutableStateOf(isSubscribed != null && !isSubscribed
                && !listOf(Route.Auth.route, Route.Premium.route).contains(currRoute?.destination?.route))
    }
    val filteredTopLevelRoutes by remember(showPremiumActionButton) {
        mutableStateOf(defaultTopLevelRoutes.filterNot { it == Route.Premium && !showPremiumActionButton })
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarController.hostState) {
                CustomSnackbar(snackbarHostState = snackbarController.hostState,
                    onDismiss = { snackbarController.hostState.currentSnackbarData?.dismiss() })
            }
        },
        bottomBar = {
            if(currRoute?.destination?.route in filteredTopLevelRoutes.map { it.route }) {
                BottomBar(
                    routes = filteredTopLevelRoutes,
                    isThemeDark = isThemeDark,
                    currRoute = currRoute?.destination?.route ?: Route.Auth.route,
                    onNavigateToRoute = {
                        if (it == Route.Premium) {
                            navController.navigate(it.route)
                        } else {
                            navController.navigate(it.route) {
                                popUpTo(navController.graph.id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    })
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
                    .clearFocusOnNonButtonClick(LocalFocusManager.current)
                    .imePadding()
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
    routes: List<Route>,
    isThemeDark: Boolean,
    currRoute: String,
    onNavigateToRoute: (Route) -> Unit
) {
    NavigationBar(
        modifier = Modifier.padding(0.dp)
    ) {
        routes.forEach { route ->
            val colors = if (route == Route.Premium) NavigationBarItemDefaults.colors(
                unselectedIconColor = if (isThemeDark) Color(0xFFFFA000) else Color(0xFFFFC107)
            ) else NavigationBarItemDefaults.colors()
            NavigationBarItem(
                selected = currRoute == route.route,
                colors = colors,
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
    WeatherSyncTheme(darkTheme = false) {
        Surface {
            BottomBar(
                routes = defaultTopLevelRoutes,
                isThemeDark = true,
                currRoute = Route.Home.route,
                onNavigateToRoute = {})
        }
    }
}