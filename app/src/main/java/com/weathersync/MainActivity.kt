package com.weathersync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.lifecycleScope
import com.weathersync.common.ui.CustomSnackbar
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.SnackbarController
import com.weathersync.features.navigation.NavManager
import com.weathersync.ui.theme.WeatherSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())
            val snackbarHostState = remember { SnackbarHostState() }
            val snackbarController by remember {
                mutableStateOf(SnackbarController(
                    context = applicationContext,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = lifecycleScope))
            }
            WeatherSyncTheme {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) {
                            CustomSnackbar(snackbarHostState = snackbarHostState, onDismiss = { snackbarHostState.currentSnackbarData?.dismiss() })
                        }
                    },
                    modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
                        NavManager(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .consumeWindowInsets(innerPadding)
                                .windowInsetsPadding(WindowInsets.ime)
                                .clearFocusOnNonButtonClick(LocalFocusManager.current)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.clearFocusOnNonButtonClick(focusManager: FocusManager) =
    this.pointerInput(Unit) {
        // Clear focus when a click event is triggered (text fields and buttons are not included)
        // focused will still be cleared when a text field is disabled
        awaitEachGesture {
            val downEvent = awaitFirstDown(pass = PointerEventPass.Initial)
            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (upEvent != null && !downEvent.isConsumed) focusManager.clearFocus(true)
        }
    }