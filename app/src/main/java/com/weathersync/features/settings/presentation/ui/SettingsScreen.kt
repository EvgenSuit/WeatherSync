package com.weathersync.features.settings.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.weathersync.R
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.CustomLinearProgressIndicator
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.UIEvent
import com.weathersync.features.settings.data.Dark
import com.weathersync.features.settings.presentation.SettingsUiState
import com.weathersync.features.settings.presentation.SettingsViewModel
import com.weathersync.features.settings.presentation.ui.components.CommonSettingsComponent
import com.weathersync.features.settings.presentation.ui.components.ThemeSwitcher
import com.weathersync.features.settings.presentation.ui.components.WeatherUnitsComponent
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.isInProgress
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val snackbar = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()
    val isThemeDark by viewModel.themeState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is UIEvent.ShowSnackbar -> snackbar.showSnackbar(event.message)
            }
        }
    }
    SettingsScreenContent(
        uiState = uiState,
        isThemeDark = isThemeDark,
        onIntent = viewModel::handleIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    isThemeDark: Dark?,
    onIntent: (SettingsIntent) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = uiState.weatherUnitsRefreshResult.isInProgress(),
        onRefresh = { onIntent(SettingsIntent.FetchWeatherUnits(refresh = true)) }) {
        ConstrainedComponent {
            if (listOf(uiState.weatherUnitsFetchResult,
                    uiState.weatherUnitsRefreshResult,
                    uiState.weatherUnitSetResult).any { it.isInProgress() }) {
                CustomLinearProgressIndicator(modifier = Modifier.testTag("Loading"))
            } else Box(modifier = Modifier.height(dimensionResource(id = R.dimen.linear_progress_height)))
            CommonSettingsComponent(textId = R.string.theme) {
                ThemeSwitcher(darkTheme = isThemeDark, onClick = { onIntent(SettingsIntent.SwitchTheme) })
            }
            WeatherUnitsComponent(
                enabled = !uiState.weatherUnitSetResult.isInProgress(),
                selectedWeatherUnits = uiState.weatherUnits,
                onWeatherUnitSelected = { onIntent(SettingsIntent.SetWeatherUnit(it)) })
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            SettingsScreenContent(
                uiState = SettingsUiState(),
                isThemeDark = true
            ) {

            }
        }
    }
}