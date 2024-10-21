package com.weathersync.features.settings.presentation.ui

import com.weathersync.features.settings.data.Dark

sealed class SettingsIntent {
    data object SwitchTheme: SettingsIntent()

}