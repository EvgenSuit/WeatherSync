package com.weathersync.features.settings.presentation.ui

import com.weathersync.features.settings.data.Dark
import com.weathersync.features.settings.data.WeatherUnit

sealed class SettingsIntent {
    data object SwitchTheme: SettingsIntent()
    data class SetWeatherUnit(val unit: WeatherUnit): SettingsIntent()
    data class FetchWeatherUnits(val refresh: Boolean): SettingsIntent()
    data object SignOut: SettingsIntent()
}