package com.weathersync.features.activityPlanning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.R
import com.weathersync.common.ui.TextFieldState
import com.weathersync.ui.UIEvent
import com.weathersync.common.ui.UIText
import com.weathersync.features.activityPlanning.ActivityPlanningRepository
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.CustomResult
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.weather.Limit
import com.weathersync.utils.weather.NextUpdateTimeFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val maxActivityInputLength = 200
class ActivityPlanningViewModel(
    private val activityPlanningRepository: ActivityPlanningRepository,
    private val analyticsManager: AnalyticsManager,
    private val nextUpdateTimeFormatter: NextUpdateTimeFormatter
): ViewModel() {
    private val _uiState = MutableStateFlow(ActivityPlanningUIState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun handleIntent(intent: ActivityPlanningIntent) {
        when (intent) {
            is ActivityPlanningIntent.GenerateRecommendations -> generateRecommendations()
            is ActivityPlanningIntent.Input -> performInput(intent.text)
        }
    }
    private fun generateRecommendations() {
        updateGenerationResult(CustomResult.InProgress)
        viewModelScope.launch {
            val input = _uiState.value.activityTextFieldState.value
            try {
                val isSubscribed = activityPlanningRepository.isSubscribed()
                val limit = activityPlanningRepository.calculateLimit(isSubscribed = isSubscribed)
                if (limit.isReached) analyticsManager.logEvent(FirebaseEvent.ACTIVITY_PLANNING_LIMIT,
                    "next_generation_time" to (limit.nextUpdateDateTime?.toString() ?: ""))
                val formattedNextUpdateTime = limit.nextUpdateDateTime?.let { nextUpdateTimeFormatter.formatNextUpdateDateTime(it) }
                _uiState.update { it.copy(limit = limit,
                    formattedNextGenerationTime = formattedNextUpdateTime) }

                if (!limit.isReached) {
                    val forecast = activityPlanningRepository.getForecast(isSubscribed = isSubscribed)
                    val suggestions = activityPlanningRepository.generateRecommendations(activity = input, forecast = forecast)
                    activityPlanningRepository.recordTimestamp()
                    analyticsManager.logEvent(FirebaseEvent.PLAN_ACTIVITIES)
                    _uiState.update { it.copy(generatedText = suggestions, forecastDays = forecast.forecastDays) }
                }
                updateGenerationResult(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvent.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_plan_activities)))
                analyticsManager.recordException(e, "Input: $input")
                updateGenerationResult(CustomResult.Error)
            }
        }
    }
    private fun performInput(text: String) =
        _uiState.update { it.copy(activityTextFieldState = it.activityTextFieldState.copy(
            value = text.take(maxActivityInputLength),
            error = if (text.isBlank())
                UIText.StringResource(R.string.input_cannot_be_empty)
            else UIText.Empty
        )) }
    private fun updateGenerationResult(result: CustomResult) =
        _uiState.update { it.copy(generationResult = result) }
}
data class ActivityPlanningUIState(
    val activityTextFieldState: TextFieldState = TextFieldState(),
    val limit: Limit = Limit(isReached = true),
    val formattedNextGenerationTime: String? = null,
    val generatedText: String? = null,
    val forecastDays: Int? = null,
    val generationResult: CustomResult = CustomResult.None
)