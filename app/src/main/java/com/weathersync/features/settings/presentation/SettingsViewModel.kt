package com.weathersync.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.R
import com.weathersync.common.ui.UIEvent
import com.weathersync.common.ui.UIText
import com.weathersync.features.settings.SettingsRepository
import com.weathersync.features.settings.data.Dark
import com.weathersync.features.settings.presentation.ui.SettingsIntent
import com.weathersync.utils.CrashlyticsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val crashlyticsManager: CrashlyticsManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = _uiState.flatMapLatest {
        settingsRepository.themeFlow(true).map {
            SettingsUiState(isThemeDark = it)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    private val _uiEvents = MutableSharedFlow<UIEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun handleIntent(settingsIntent: SettingsIntent) {
        when (settingsIntent) {
            is SettingsIntent.SwitchTheme -> switchTheme()
        }
    }

    private fun switchTheme() = viewModelScope.launch {
        try {
            settingsRepository.setTheme(!uiState.first().isThemeDark!!)
        } catch (e: Exception) {
            _uiEvents.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_switch_theme)))
            crashlyticsManager.recordException(e, "Is theme dark: ${_uiState.value.isThemeDark}")
        }
    }

}

data class SettingsUiState(
    val isThemeDark: Dark? = null
)