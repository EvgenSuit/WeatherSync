package com.weathersync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.SnackbarController
import com.weathersync.features.navigation.presentation.ui.NavManager
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.subscription.SubscriptionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val themeManager: ThemeManager by inject()
    private val subscriptionManager: SubscriptionManager by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val didCallBillingInitMethod = mutableStateOf(false)

    private fun initBillingClient() = lifecycleScope.launch {
        try {
            subscriptionManager.initBillingClient()
        } catch (e: Exception) {
            analyticsManager.recordException(e)
        } finally {
            didCallBillingInitMethod.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        // https://developer.android.com/google/play/billing/integrate#fetch
        initBillingClient()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition {
            !didCallBillingInitMethod.value
        }
        enableEdgeToEdge()

        val initTheme = runBlocking { themeManager.themeFlow(true).first() }
        setContent {
            val isThemeDark by themeManager.themeFlow(true).collectAsState(initial = initTheme)
            window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())
            val snackbarHostState = remember { SnackbarHostState() }
            val snackbarController by remember {
                mutableStateOf(SnackbarController(
                    context = applicationContext,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = lifecycleScope))
            }
            WeatherSyncTheme(isThemeDark) {
                CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
                    NavManager(
                        activity = this
                    )
                }
            }
        }
    }
}

@Composable
fun Modifier.clearFocusOnNonButtonClick(focusManager: FocusManager) =
    this.pointerInput(Unit) {
        // Clear focus when a click event is triggered (text fields and buttons are not included)
        // focus will still be cleared when a text field is disabled
        awaitEachGesture {
            val downEvent = awaitFirstDown(pass = PointerEventPass.Initial)
            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (upEvent != null && !downEvent.isConsumed) focusManager.clearFocus(true)
        }
    }