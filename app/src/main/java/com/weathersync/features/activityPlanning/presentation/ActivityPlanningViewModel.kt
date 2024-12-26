package com.weathersync.features.activityPlanning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.R
import com.weathersync.common.ui.TextFieldState
import com.weathersync.ui.UIEvent
import com.weathersync.common.ui.UIText
import com.weathersync.features.activityPlanning.ForecastDays
import com.weathersync.features.activityPlanning.domain.ActivityPlanningRepository
import com.weathersync.ui.ActivityPlanningUIEvent
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.CustomResult
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.weather.limits.Limit
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val maxActivityInputLength = 400
class ActivityPlanningViewModel(
    private val activityPlanningRepository: ActivityPlanningRepository,
    private val analyticsManager: AnalyticsManager,
    private val nextUpdateTimeFormatter: NextUpdateTimeFormatter,
    subscriptionInfoDatastore: SubscriptionInfoDatastore
): ViewModel() {
    private val _uiState = MutableStateFlow(ActivityPlanningUIState())
    val uiState = _uiState.asStateFlow()

    val showBannerAds = subscriptionInfoDatastore.isSubscribedFlow()
        .map { it?.not() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _uiEvent = MutableSharedFlow<ActivityPlanningUIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        refreshLimits()
    }

    fun handleIntent(intent: ActivityPlanningIntent) {
        when (intent) {
            is ActivityPlanningIntent.RefreshLimits -> refreshLimits()
            is ActivityPlanningIntent.GenerateRecommendations -> generateRecommendations()
            is ActivityPlanningIntent.Input -> performInput(intent.text)
            is ActivityPlanningIntent.NavigateToPremium -> viewModelScope.launch {
                _uiEvent.emit(ActivityPlanningUIEvent.NavigateToPremium)
            }
        }
    }

    private fun refreshLimits() {
        viewModelScope.launch {
            try {
                updateLimitsRefreshResult(CustomResult.InProgress)
                val isSubscribed = activityPlanningRepository.isSubscribed()
                val limit = activityPlanningRepository.calculateLimit(isSubscribed = isSubscribed)
                if (limit.isReached) analyticsManager.logEvent(FirebaseEvent.ACTIVITY_PLANNING_LIMIT,
                    showInterstitialAd = null,
                    "next_generation_time" to (limit.nextUpdateDateTime?.toString() ?: ""))
                val formattedNextGenerationTime = limit.nextUpdateDateTime?.let { nextUpdateTimeFormatter.format(it) }
                _uiState.update { it.copy(limit = limit,
                    forecastDays = (if (isSubscribed) ForecastDays.PREMIUM else ForecastDays.REGULAR).days,
                    formattedNextGenerationTime = formattedNextGenerationTime) }
                updateLimitsRefreshResult(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvent.emit(ActivityPlanningUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_refresh_limits)))
                analyticsManager.recordException(e)
                updateLimitsRefreshResult(CustomResult.Error)
            }
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
                    showInterstitialAd = null,
                    "next_generation_time" to (limit.nextUpdateDateTime?.toString() ?: ""))
                val formattedNextGenerationTime = limit.nextUpdateDateTime?.let { nextUpdateTimeFormatter.format(it) }
                _uiState.update { it.copy(limit = limit,
                    formattedNextGenerationTime = formattedNextGenerationTime) }

                if (!limit.isReached) {
                    val forecast = activityPlanningRepository.getForecast(isSubscribed = isSubscribed)
                    val suggestions = activityPlanningRepository.generateRecommendations(
                        activity = input,
                        isSubscribed = isSubscribed,
                        forecast = forecast)
                    activityPlanningRepository.recordTimestamp()
                    analyticsManager.logEvent(
                        event = FirebaseEvent.PLAN_ACTIVITIES,
                        showInterstitialAd = !isSubscribed,
                        "suggestions" to suggestions)
                    _uiState.update { it.copy(
                        generatedText = suggestions,
                        forecastDays = forecast.forecastDays) }
                }
                updateGenerationResult(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvent.emit(ActivityPlanningUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_plan_activities)))
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

    private fun updateLimitsRefreshResult(result: CustomResult) =
        _uiState.update { it.copy(limitsRefreshResult = result) }
    private fun updateGenerationResult(result: CustomResult) =
        _uiState.update { it.copy(generationResult = result) }
}
data class ActivityPlanningUIState(
    val activityTextFieldState: TextFieldState = TextFieldState(),
    val limit: Limit = Limit(isReached = true),
    val formattedNextGenerationTime: String? = null,
    val generatedText: String? = null,
    val forecastDays: Int? = null,
    val limitsRefreshResult: CustomResult = CustomResult.None,
    val generationResult: CustomResult = CustomResult.None
)