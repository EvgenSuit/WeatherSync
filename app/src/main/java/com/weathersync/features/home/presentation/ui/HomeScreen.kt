package com.weathersync.features.home.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.weathersync.R
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.ui.UIEvent
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.presentation.HomeUIState
import com.weathersync.features.home.presentation.HomeViewModel
import com.weathersync.features.home.presentation.LocationRequester
import com.weathersync.features.home.presentation.ui.components.CurrentWeatherComposable
import com.weathersync.features.home.presentation.ui.components.RecommendedActivitiesComposable
import com.weathersync.features.home.presentation.ui.components.WhatToWearComposable
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.Limit
import com.weathersync.utils.isInProgress
import com.weathersync.utils.isNone
import com.weathersync.utils.isSuccess
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.util.Date

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val snackbarController = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvent.ShowSnackbar -> snackbarController.showSnackbar(event.message)
            }
        }
    }
    LocationRequester(onPermissionGranted = {
        if (uiState.currentWeatherFetchResult.isNone()) viewModel.handleIntent(HomeIntent.GetCurrentWeather)
    })
    HomeScreenContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent
        )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUIState,
    onIntent: (HomeIntent) -> Unit
) {
    val nextUpdateTime = uiState.limit.formattedNextUpdateTime
    val suggestions = uiState.suggestions
    PullToRefreshBox(
        isRefreshing = uiState.currentWeatherRefreshResult.isInProgress(),
        onRefresh = { onIntent(HomeIntent.RefreshCurrentWeather) },
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()) {
        ConstrainedComponent {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                nextUpdateTime?.let { Text(text = stringResource(id = R.string.next_update_time, it)) }
            }
            CurrentWeatherComposable(
                weather = uiState.currentWeather,
                isFetchInProgress = listOf(uiState.currentWeatherFetchResult, uiState.currentWeatherRefreshResult).any { it.isInProgress() })
            RecommendedActivitiesComposable(
                recommendedActivities = suggestions?.recommendedActivities,
                unrecommendedActivities = suggestions?.unrecommendedActivities,
                isGenerationSuccessful = uiState.suggestionsGenerationResult.isSuccess()
            )
            WhatToWearComposable(
                recommendations = suggestions?.whatToBring,
                isGenerationSuccessful = uiState.suggestionsGenerationResult.isSuccess()
            )
        }
    }
}

@Preview(device = "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240")
@Composable
fun HomeScreenPreview() {
    WeatherSyncTheme {
        Surface {
            HomeScreenContent(
                uiState = HomeUIState(
                    limit = Limit(isReached = true, formattedNextUpdateTime = Date.from(Instant.now().plusSeconds(24*60*60)).toString())
                ),
                onIntent = {}
            )
        }
    }
}