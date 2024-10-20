package com.weathersync.features.activityPlanning.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.R
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.UIEvent
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningIntent
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningUIState
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import com.weathersync.features.activityPlanning.presentation.ui.components.PlanYourActivityComposable
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.Limit
import com.weathersync.utils.isInProgress
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.util.Date

@Composable
fun ActivityPlanningScreen(
    viewModel: ActivityPlanningViewModel = koinViewModel()
) {
    val snackbarController = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
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
    val nextUpdateTime = uiState.limit.formattedNextUpdateTime
    val output = uiState.generatedText
    ConstrainedComponent {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                nextUpdateTime?.let { Text(text = stringResource(id = R.string.next_generation_available_at, it),
                    modifier = Modifier.testTag("Next generation time")) }
            }
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
                    generatedText = "Generated suggestions".repeat(30),
                    limit = Limit(isReached = true, formattedNextUpdateTime = Date.from(Instant.now().plusSeconds(24*60*60)).toString())
                ),
                onIntent = {}
            )
        }
    }
}