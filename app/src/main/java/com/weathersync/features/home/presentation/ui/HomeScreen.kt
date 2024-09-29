package com.weathersync.features.home.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.UIEvent
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.presentation.HomeUIState
import com.weathersync.features.home.presentation.HomeViewModel
import com.weathersync.features.home.presentation.LocationRequester
import com.weathersync.features.home.presentation.ui.components.CurrentWeatherComposable
import com.weathersync.features.home.presentation.ui.components.RecommendedActivitiesComposable
import com.weathersync.features.home.presentation.ui.components.WhatToWearComposable
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.isInProgress
import com.weathersync.utils.isSuccess
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val snackbarController = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UIEvent.ShowSnackbar -> snackbarController.showSnackbar(event.message)
            }
        }
    }
    LocationRequester(onPermissionGranted = {
        viewModel.handleIntent(HomeIntent.GetCurrentWeather)
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
    val suggestions = uiState.suggestions
    PullToRefreshBox(
        isRefreshing = uiState.currentWeatherRefreshResult.isInProgress(),
        onRefresh = { onIntent(HomeIntent.RefreshCurrentWeather) },
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // without verticalScroll pull to refresh will not work
        ) {
            ConstrainedComponent {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        //.width(dimensionResource(id = R.dimen.max_width))
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    CurrentWeatherComposable(
                        weather = uiState.currentWeather,
                        isFetchInProgress = listOf(uiState.currentWeatherFetchResult, uiState.currentWeatherRefreshResult).any { it.isInProgress() })
                    RecommendedActivitiesComposable(
                        recommendedActivities = suggestions.recommendedActivities,
                        unrecommendedActivities = suggestions.unrecommendedActivities,
                        isGenerationSuccessful = uiState.suggestionsGenerationResult.isSuccess()
                    )
                    WhatToWearComposable(
                        recommendations = suggestions.whatToBring,
                        isGenerationSuccessful = uiState.suggestionsGenerationResult.isSuccess()
                    )
                }
            }
        }
    }
}

@Preview(device = "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240")
@Composable
fun HomeScreenPreview() {
    WeatherSyncTheme {
        Surface {
            HomeScreenContent(
                uiState = HomeUIState(),
                onIntent = {}
            )
        }
    }
}