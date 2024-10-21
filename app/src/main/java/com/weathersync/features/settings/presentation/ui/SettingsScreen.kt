package com.weathersync.features.settings.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.UIEvent
import com.weathersync.features.settings.presentation.SettingsUiState
import com.weathersync.features.settings.presentation.SettingsViewModel
import com.weathersync.features.settings.presentation.ui.components.ThemeSwitcher
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val snackbar = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is UIEvent.ShowSnackbar -> snackbar.showSnackbar(event.message)
            }
        }
    }
    SettingsScreenContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit
) {
    ConstrainedComponent {
        Box(modifier = Modifier.fillMaxWidth()) {
            ThemeSwitcher(darkTheme = uiState.isThemeDark, onClick = { onIntent(SettingsIntent.SwitchTheme) })
        }
    }
}