package com.weathersync.features.activityPlanning.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
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
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningIntent
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningUIState
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import com.weathersync.features.activityPlanning.presentation.ui.components.PlanYourActivityComposable
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.isInProgress
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ActivityPlanningScreen(
    viewModel: ActivityPlanningViewModel = koinViewModel()
) {
    val snackbarController = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UIEvent.ShowSnackbar -> { snackbarController.showSnackbar(event.message) }
            }
        }
    }
    ActivityPlanningScreenContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent
    )
}

@Composable
fun ActivityPlanningScreenContent(
    uiState: ActivityPlanningUIState,
    onIntent: (ActivityPlanningIntent) -> Unit
) {
    val output = uiState.generatedText
    ConstrainedComponent {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            PlanYourActivityComposable(
                textFieldState = uiState.activityTextFieldState,
                isInProgress = uiState.generationResult.isInProgress(),
                output = output,
                onIntent = onIntent
            )
        }
    }
}

@Preview
@Composable
fun ActivityPlanningScreenContentPreview() {
    WeatherSyncTheme {
        Surface {
            ActivityPlanningScreenContent(
                uiState = ActivityPlanningUIState(
                    generatedText = "Generated suggestions".repeat(30)
                ),
                onIntent = {}
            )
        }
    }
}