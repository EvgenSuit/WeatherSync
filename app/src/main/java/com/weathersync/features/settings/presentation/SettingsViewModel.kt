package com.weathersync.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.R
import com.weathersync.common.ui.UIText
import com.weathersync.features.settings.SettingsRepository
import com.weathersync.features.settings.data.SelectedWeatherUnits
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.features.settings.presentation.ui.SettingsIntent
import com.weathersync.ui.SettingsUIEvent
import com.weathersync.utils.CrashlyticsManager
import com.weathersync.utils.CustomResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val crashlyticsManager: CrashlyticsManager
) : ViewModel() {
    private val _uiEvents = MutableSharedFlow<SettingsUIEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    val themeState = settingsRepository.themeFlow(isDarkByDefault = true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchWeatherUnits(refresh = false)
    }

    fun handleIntent(settingsIntent: SettingsIntent) {
        when (settingsIntent) {
            is SettingsIntent.SwitchTheme -> switchTheme()
            is SettingsIntent.FetchWeatherUnits -> fetchWeatherUnits(refresh = settingsIntent.refresh)
            is SettingsIntent.SetWeatherUnit -> setWeatherUnit(settingsIntent.unit)
            is SettingsIntent.SignOut -> signOut()
        }
    }

    private fun switchTheme() = viewModelScope.launch {
        try {
            settingsRepository.setTheme(!themeState.value!!)
        } catch (e: Exception) {
            _uiEvents.emit(SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_switch_theme)))
            crashlyticsManager.recordException(e, "Is theme dark: ${themeState.value}")
        }
    }
    private fun fetchWeatherUnits(refresh: Boolean) {
        val update: (CustomResult) -> Unit = { if (refresh) updateUnitRefreshResult(it)
        else updateUnitFetchResult(it) }
        update(CustomResult.InProgress)
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(weatherUnits = settingsRepository.getUnits()) }
                update(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvents.emit(SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_load_units)))
                crashlyticsManager.recordException(e)
                update(CustomResult.Error)
            }
        }
    }
    private fun setWeatherUnit(unit: WeatherUnit) {
        updateUnitSetResult(CustomResult.InProgress)
        viewModelScope.launch {
            try {
                settingsRepository.setWeatherUnit(unit)
                val currentUnits = uiState.value.weatherUnits ?: SelectedWeatherUnits(
                    temp = WeatherUnit.Temperature.Celsius,
                    windSpeed = WeatherUnit.WindSpeed.KMH,
                    visibility = WeatherUnit.Visibility.Kilometers
                )
                val updatedUnits = when (unit) {
                    is WeatherUnit.Temperature -> currentUnits.copy(temp = unit)
                    is WeatherUnit.WindSpeed -> currentUnits.copy(windSpeed = unit)
                    is WeatherUnit.Visibility -> currentUnits.copy(visibility = unit)
                }
                _uiState.update { it.copy(weatherUnits = updatedUnits) }
                updateUnitSetResult(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvents.emit(SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_set_units)))
                crashlyticsManager.recordException(e)
                updateUnitSetResult(CustomResult.Error)
            }
        }
    }
    private fun signOut() {
        viewModelScope.launch {
            settingsRepository.signOut()
            _uiEvents.emit(SettingsUIEvent.SignOut)
        }
    }

    private fun updateUnitFetchResult(result: CustomResult) =
        _uiState.update { it.copy(weatherUnitsFetchResult = result) }
    private fun updateUnitRefreshResult(result: CustomResult) =
        _uiState.update { it.copy(weatherUnitsRefreshResult = result) }
    private fun updateUnitSetResult(result: CustomResult) =
        _uiState.update { it.copy(weatherUnitSetResult = result) }

}

data class SettingsUiState(
    val weatherUnits: SelectedWeatherUnits? = null,
    val weatherUnitsFetchResult: CustomResult = CustomResult.None,
    val weatherUnitsRefreshResult: CustomResult = CustomResult.None,
    val weatherUnitSetResult: CustomResult = CustomResult.None
)