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
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.CustomResult
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.NoGoogleMapsGeocodingResult
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
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
    private val analyticsManager: AnalyticsManager,
    private val nextUpdateTimeFormatter: NextUpdateTimeFormatter,
    subscriptionInfoDatastore: SubscriptionInfoDatastore,
) : ViewModel() {
    private val _uiEvents = MutableSharedFlow<SettingsUIEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    val themeState = settingsRepository.themeFlow(isDarkByDefault = true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val isSubscribed = subscriptionInfoDatastore.isSubscribedFlow()
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
            is SettingsIntent.ManageSetLocationSheet -> viewModelScope.launch {
                _uiEvents.emit(SettingsUIEvent.ManageSetLocationSheet(settingsIntent.show))
            }
            is SettingsIntent.SetLocation -> setLocation(settingsIntent.location)
            is SettingsIntent.SetCurrLocationAsDefault -> setLocation(null)
            is SettingsIntent.SignOut -> signOut()
        }
    }

    private fun switchTheme() = viewModelScope.launch {
        try {
            settingsRepository.setTheme(!themeState.value!!)
        } catch (e: Exception) {
            _uiEvents.emit(SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_switch_theme)))
            analyticsManager.recordException(e, "Is theme dark: ${themeState.value}")
        }
    }
    private fun fetchWeatherUnits(refresh: Boolean) {
        val update: (CustomResult) -> Unit = { if (refresh) updateUnitRefreshResult(it)
        else updateUnitFetchResult(it) }
        update(CustomResult.InProgress)
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(weatherUnits = settingsRepository.getUnits()) }
                analyticsManager.logEvent(event = FirebaseEvent.FETCH_WEATHER_UNITS)
                update(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvents.emit(SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_load_units)))
                analyticsManager.recordException(e)
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
                analyticsManager.logEvent(FirebaseEvent.CHANGE_WEATHER_UNITS,
                    showInterstitialAd = null,
                    "temp" to updatedUnits.temp.unitName,
                    "windSpeed" to updatedUnits.windSpeed.unitName,
                    "visibility" to updatedUnits.visibility.unitName)
                _uiState.update { it.copy(weatherUnits = updatedUnits) }
                updateUnitSetResult(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvents.emit(SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_set_units)))
                analyticsManager.recordException(e)
                updateUnitSetResult(CustomResult.Error)
            }
        }
    }

    private fun setLocation(inputLocation: String?) {
        viewModelScope.launch {
            try {
                updateLocationSetResult(CustomResult.InProgress)

                // the fact that inputLocation is not null signifies that a premium user is setting a custom worldwide location,
                // not the current one (which any user can do)
                val newLocationName = if (inputLocation != null) {
                    val isSubscribed = settingsRepository.isSubscribed()
                    val limit = settingsRepository.calculateLocationSetLimits(isSubscribed)
                    if (limit.isReached) {
                        _uiState.update { it.copy(nextWorldwideSetTime = nextUpdateTimeFormatter.format(limit.nextUpdateDateTime!!)) }
                        analyticsManager.logEvent(event = FirebaseEvent.SET_CUSTOM_LOCATION_LIMIT)
                        updateLocationSetResult(CustomResult.None)
                        return@launch
                    }
                    settingsRepository.setLocation(inputLocation)
                } else settingsRepository.setCurrLocationAsDefault()

                if (inputLocation != null) settingsRepository.incrementLocationSetLimits()
                analyticsManager.logEvent(event = if (inputLocation != null) FirebaseEvent.SET_CUSTOM_LOCATION else FirebaseEvent.SET_CURR_LOCATION_AS_DEFAULT)
                updateLocationSetResult(CustomResult.Success)
                _uiEvents.emit(SettingsUIEvent.ManageSetLocationSheet(show = false))
                _uiEvents.emit(SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.location_set_successfully, newLocationName)))
            } catch (e: Exception) {
                // make sure limits are incremented even when there are no results for given location
                if (e is NoGoogleMapsGeocodingResult) settingsRepository.incrementLocationSetLimits()
                _uiEvents.emit(SettingsUIEvent.ManageSetLocationSheet(show = false))
                _uiEvents.emit(SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_set_location)))
                analyticsManager.recordException(e)
                updateLocationSetResult(CustomResult.Error)
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            settingsRepository.signOut()
            analyticsManager.logEvent(event = FirebaseEvent.SIGN_OUT)
            _uiEvents.emit(SettingsUIEvent.SignOut)
        }
    }

    private fun updateUnitFetchResult(result: CustomResult) =
        _uiState.update { it.copy(weatherUnitsFetchResult = result) }
    private fun updateUnitRefreshResult(result: CustomResult) =
        _uiState.update { it.copy(weatherUnitsRefreshResult = result) }
    private fun updateUnitSetResult(result: CustomResult) =
        _uiState.update { it.copy(weatherUnitSetResult = result) }
    private fun updateLocationSetResult(result: CustomResult) =
        _uiState.update { it.copy(locationSetResult = result) }
}
data class SettingsUiState(
    val weatherUnits: SelectedWeatherUnits? = null,
    val weatherUnitsFetchResult: CustomResult = CustomResult.None,
    val weatherUnitsRefreshResult: CustomResult = CustomResult.None,
    val weatherUnitSetResult: CustomResult = CustomResult.None,
    val nextWorldwideSetTime: String? = null,
    val locationSetResult: CustomResult = CustomResult.None
)