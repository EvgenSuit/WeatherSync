package com.weathersync.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.R
import com.weathersync.common.ui.UIEvent
import com.weathersync.common.ui.UIText
import com.weathersync.features.home.HomeRepository
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.CrashlyticsManager
import com.weathersync.utils.CustomResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val crashlyticsManager: CrashlyticsManager,
): ViewModel() {
    private val scope = viewModelScope
    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState = _uiState.asStateFlow()

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

        updateMethod(CustomResult.InProgress)
        scope.launch {
            try {
                val weather = homeRepository.getCurrentWeather()
                _uiState.update { it.copy(currentWeather = weather) }
                updateMethod(CustomResult.Success())
            } catch (e: Exception) {
                _uiEvent.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_fetch_current_weather)))
                crashlyticsManager.recordException(e, "Is refreshing: $refresh")
                updateMethod(CustomResult.Error)
            }
        }
    }
    private fun updateCurrentWeatherRefreshResult(result: CustomResult) =
        _uiState.update { it.copy(currentWeatherRefreshResult = result) }
    private fun updateCurrentWeatherFetchResult(result: CustomResult) =
        _uiState.update { it.copy(currentWeatherFetchResult = result) }

}
data class HomeUIState(
    val currentWeather: CurrentWeather? = null,
    val currentWeatherRefreshResult: CustomResult = CustomResult.None,
    val currentWeatherFetchResult: CustomResult = CustomResult.None
)