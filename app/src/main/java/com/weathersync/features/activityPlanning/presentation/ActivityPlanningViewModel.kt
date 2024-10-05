package com.weathersync.features.activityPlanning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.R
import com.weathersync.common.ui.TextFieldState
import com.weathersync.common.ui.UIEvent
import com.weathersync.common.ui.UIText
import com.weathersync.features.activityPlanning.ActivityPlanningRepository
import com.weathersync.utils.CrashlyticsManager
import com.weathersync.utils.CustomResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val maxActivityInputLength = 200
class ActivityPlanningViewModel(
    private val activityPlanningRepository: ActivityPlanningRepository,
    private val crashlyticsManager: CrashlyticsManager
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
                val times = activityPlanningRepository.generateRecommendations(activity = input)
                _uiState.update { it.copy(generatedText = times) }
                updateGenerationResult(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvent.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_plan_activities)))
                crashlyticsManager.recordException(e, "Input: $input")
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
    val generatedText: String = "",
    val generationResult: CustomResult = CustomResult.None
)