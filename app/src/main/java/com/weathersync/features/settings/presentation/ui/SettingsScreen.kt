package com.weathersync.features.settings.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.R
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.CustomLinearProgressIndicator
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.PrivacyTermsLinks
import com.weathersync.common.ui.openLinkInBrowser
import com.weathersync.features.settings.data.Dark
import com.weathersync.features.settings.presentation.SettingsUiState
import com.weathersync.features.settings.presentation.SettingsViewModel
import com.weathersync.features.settings.presentation.ui.components.AppVersionComponent
import com.weathersync.features.settings.presentation.ui.components.CommonSettingsComponent
import com.weathersync.features.settings.presentation.ui.components.ThemeSwitcher
import com.weathersync.features.settings.presentation.ui.components.WeatherUnitsComponent
import com.weathersync.ui.SettingsUIEvent
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.isInProgress
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onSignOut: () -> Unit
) {
    val snackbar = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()
    val isThemeDark by viewModel.themeState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is SettingsUIEvent.ShowSnackbar -> snackbar.showSnackbar(event.message)
                is SettingsUIEvent.SignOut -> onSignOut()
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
    val context = LocalContext.current
    val subscriptionUrl = stringResource(id = R.string.manage_subscriptions_link)
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
                ThemeSwitcher(
                    darkTheme = isThemeDark,
                    onClick = { onIntent(SettingsIntent.SwitchTheme) })
            }
            WeatherUnitsComponent(
                enabled = !uiState.weatherUnitSetResult.isInProgress(),
                selectedWeatherUnits = uiState.weatherUnits,
                onWeatherUnitSelected = { onIntent(SettingsIntent.SetWeatherUnit(it)) })
            CommonSettingsComponent(
                textId = R.string.manage_subscriptions,
                onClick = { openLinkInBrowser(context, subscriptionUrl) }) {
                val icon = Icons.Filled.ArrowOutward
                Icon(imageVector = icon, contentDescription = icon.name)
            }
            TextButton(
                onClick = { onIntent(SettingsIntent.SignOut) },
                modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)) {
                    Text(text = stringResource(id = R.string.sign_out),
                        style = MaterialTheme.typography.displayMedium)
                }
            }
            HorizontalDivider(modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(top = 15.dp))
            AppVersionComponent()
            PrivacyTermsLinks()
        }
    }
}

@Preview(device = "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420")
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