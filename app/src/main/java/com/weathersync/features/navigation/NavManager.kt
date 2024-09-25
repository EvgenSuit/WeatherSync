package com.weathersync.features.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.weathersync.features.auth.ui.AuthScreen
import com.weathersync.features.home.ui.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object Auth
@Serializable
object Home

@Composable
fun NavManager(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier
) {
    val isUserNull = Firebase.auth.currentUser == null
    NavHost(
        navController = navController,
        startDestination = if (isUserNull) Auth else Home,
        modifier = modifier
    ) {
        composable<Auth> {
            AuthScreen(onNavigateToHome = { navController.navigate(Home) }
        ) }
        composable<Home> {
            HomeScreen()
        }
    }
}