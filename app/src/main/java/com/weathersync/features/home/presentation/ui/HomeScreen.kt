package com.weathersync.features.home.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.weathersync.R
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.UIEvent
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.presentation.HomeUIState
import com.weathersync.features.home.presentation.HomeViewModel
import com.weathersync.features.home.presentation.LocationRequester
import com.weathersync.utils.isInProgress
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
    PullToRefreshBox(
        isRefreshing = uiState.currentWeatherRefreshResult.isInProgress(),
        onRefresh = { onIntent(HomeIntent.RefreshCurrentWeather) },
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
            .padding(10.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.max_width))
                .fillMaxSize()
                .padding(10.dp)
                .verticalScroll(rememberScrollState()) // without verticalScroll pull to refresh will not work
        ) {
            CurrentWeatherComposable(
                weather = uiState.currentWeather,
                fetchResult = uiState.currentWeatherFetchResult)
        }
    }
}