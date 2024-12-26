package com.weathersync.features.activityPlanning.presentation.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.weathersync.R
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.NextGenerationComponent
import com.weathersync.common.ui.UpgradeToPremium
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningIntent
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningUIState
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import com.weathersync.features.activityPlanning.presentation.ui.components.PlanYourActivityComposable
import com.weathersync.ui.ActivityPlanningUIEvent
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.ads.AdBannerType
import com.weathersync.utils.ads.BannerAdView
import com.weathersync.utils.isInProgress
import com.weathersync.utils.weather.limits.Limit
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.util.Date

@Composable
fun ActivityPlanningScreen(
    viewModel: ActivityPlanningViewModel = koinViewModel(),
    onNavigateToPremium: () -> Unit = {}
) {
    val snackbarController = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()
    val showBannerAds by viewModel.showBannerAds.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ActivityPlanningUIEvent.ShowSnackbar -> { snackbarController.showSnackbar(event.message) }
                is ActivityPlanningUIEvent.NavigateToPremium -> onNavigateToPremium()
            }
        }
    }
    ActivityPlanningScreenContent(
        uiState = uiState,
        showBannerAds = showBannerAds,
        onIntent = viewModel::handleIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityPlanningScreenContent(
    uiState: ActivityPlanningUIState,
    showBannerAds: Boolean?,
    onIntent: (ActivityPlanningIntent) -> Unit
) {
    val formattedNextGenerationTime = uiState.formattedNextGenerationTime
    val output = uiState.generatedText
    PullToRefreshBox(isRefreshing = uiState.limitsRefreshResult.isInProgress(),
        onRefresh = { onIntent(ActivityPlanningIntent.RefreshLimits) }) {
        ConstrainedComponent {
            formattedNextGenerationTime?.let {
                NextGenerationComponent(nextUpdateTime = stringResource(id = R.string.next_generation_available_at, it)) {
                    if (showBannerAds == true) UpgradeToPremium(onClick = { onIntent(ActivityPlanningIntent.NavigateToPremium) })
                }
            }
            PlanYourActivityComposable(
                textFieldState = uiState.activityTextFieldState,
                isInProgress = uiState.generationResult.isInProgress(),
                refreshing = uiState.limitsRefreshResult.isInProgress(),
                forecastDays = uiState.forecastDays,
                output = output,
                onIntent = onIntent
            )
            if (showBannerAds == true) BannerAdView(adBannerType = AdBannerType.ActivityPlanning)
        }
    }
}

@Preview
@Composable
fun ActivityPlanningScreenContentPreview() {
    WeatherSyncTheme {
        Surface {
            ActivityPlanningScreenContent(
                showBannerAds = true,
                uiState = ActivityPlanningUIState(
                    generatedText = "Generated suggestions".repeat(30),
                    limit = Limit(isReached = true, nextUpdateDateTime = Date.from(Instant.now().plusSeconds(24*60*60)))
                ),
                onIntent = {}
            )
        }
    }
}