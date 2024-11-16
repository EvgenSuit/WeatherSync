package com.weathersync.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.R
import com.weathersync.ui.UIEvent
import com.weathersync.common.ui.UIText
import com.weathersync.features.home.HomeRepository
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.CustomResult
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.weather.limits.Limit
import com.weathersync.utils.isInProgress
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
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

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val analyticsManager: AnalyticsManager,
    private val nextUpdateTimeFormatter: NextUpdateTimeFormatter,
    subscriptionInfoDatastore: SubscriptionInfoDatastore
): ViewModel() {
    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState = _uiState.asStateFlow()

    val showBannerAds = subscriptionInfoDatastore.isSubscribedFlow()
        .map { it?.not() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun handleIntent(homeIntent: HomeIntent) {
        when (homeIntent) {
            is HomeIntent.GetCurrentWeather -> getCurrentWeather(false)
            is HomeIntent.RefreshCurrentWeather -> getCurrentWeather(true)
        }
    }
    private fun getCurrentWeather(refresh: Boolean) {
        val updateMethod: (CustomResult) -> Unit = if (refresh) { res -> updateCurrentWeatherRefreshResult(res) }
        else { res -> updateCurrentWeatherFetchResult(res) }
        val currentResult = if (refresh) uiState.value.currentWeatherRefreshResult
        else uiState.value.currentWeatherFetchResult
        if (currentResult.isInProgress()) return
        updateMethod(CustomResult.InProgress)
        viewModelScope.launch {
            try {
                val isSubscribed = homeRepository.isSubscribed()

                val limit = homeRepository.calculateLimit(isSubscribed = isSubscribed)
                if (limit.isReached) analyticsManager.logEvent(FirebaseEvent.CURRENT_WEATHER_FETCH_LIMIT,
                    isSubscribed = isSubscribed,
                    "next_update_time" to (limit.nextUpdateDateTime?.toString() ?: ""))
                val formattedNextUpdateTime = limit.nextUpdateDateTime?.let { nextUpdateTimeFormatter.formatNextUpdateDateTime(it) }
                _uiState.update { it.copy(
                    limit = limit,
                    formattedNextUpdateTime = formattedNextUpdateTime) }

                val weather = homeRepository.getCurrentWeather(isLimitReached = limit.isReached)
                analyticsManager.logEvent(event = FirebaseEvent.FETCH_CURRENT_WEATHER,
                    isSubscribed = isSubscribed,
                    "is_limit_reached" to limit.isReached.toString(),
                    "locality" to weather?.locality.orEmpty(),
                    "temp" to weather?.temp.toString(),
                    "wind_speed" to weather?.windSpeed.toString(),
                    "weather_code" to weather?.weatherCode.toString(),
                    "time" to weather?.time.orEmpty())
                _uiState.update { it.copy(currentWeather = weather) }

                updateMethod(CustomResult.Success)
                if (weather != null) generateSuggestions(
                    isLimitReached = limit.isReached,
                    isSubscribed = isSubscribed,
                    currentWeather = weather)
            } catch (e: Exception) {
                _uiEvent.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_fetch_current_weather)))
                analyticsManager.recordException(e, "Is refreshing: $refresh")
                updateMethod(CustomResult.Error)
            }
        }
    }
    private suspend fun generateSuggestions(
        isLimitReached: Boolean,
        isSubscribed: IsSubscribed,
        currentWeather: CurrentWeather) {
        updateSuggestionsGenerationResult(CustomResult.InProgress)
        try {
            val recommendations = homeRepository.generateSuggestions(isLimitReached = isLimitReached, currentWeather = currentWeather)
            // add timestamp only if current weather fetch and suggestions generation are successful
            if (!isLimitReached) homeRepository.recordTimestamp()
            analyticsManager.logEvent(event = FirebaseEvent.GENERATE_SUGGESTIONS,
                isSubscribed = isSubscribed,
                "is_limit_reached" to isLimitReached.toString(),
                "recommended_activities" to recommendations?.recommendedActivities?.joinToString(", ").orEmpty(),
                "unrecommended_activities" to recommendations?.unrecommendedActivities?.joinToString(", ").orEmpty(),
                "what_to_wear_bring" to recommendations?.whatToBring?.joinToString(", ").orEmpty()
                )
            _uiState.update { it.copy(suggestions = recommendations ?: Suggestions()) }
            updateSuggestionsGenerationResult(CustomResult.Success)
        } catch (e: Exception) {
            _uiEvent.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_generate_suggestions)))
            analyticsManager.recordException(e)
            updateSuggestionsGenerationResult(CustomResult.Error)
        }
    }

    private fun updateCurrentWeatherRefreshResult(result: CustomResult) =
        _uiState.update { it.copy(currentWeatherRefreshResult = result) }
    private fun updateCurrentWeatherFetchResult(result: CustomResult) =
        _uiState.update { it.copy(currentWeatherFetchResult = result) }
    private fun updateSuggestionsGenerationResult(result: CustomResult) =
        _uiState.update { it.copy(suggestionsGenerationResult = result) }

}
data class HomeUIState(
    val currentWeather: CurrentWeather? = null,
    val suggestions: Suggestions? = null,
    val limit: Limit = Limit(isReached = true),
    val formattedNextUpdateTime: String? = null,
    val currentWeatherRefreshResult: CustomResult = CustomResult.None,
    val currentWeatherFetchResult: CustomResult = CustomResult.None,
    val suggestionsGenerationResult: CustomResult = CustomResult.None
)