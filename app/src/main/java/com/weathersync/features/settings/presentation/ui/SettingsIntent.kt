package com.weathersync.features.settings.presentation.ui

import com.weathersync.features.settings.data.WeatherUnit

sealed class SettingsIntent {
    data object SwitchTheme: SettingsIntent()
    data class SetWeatherUnit(val unit: WeatherUnit): SettingsIntent()
    data class FetchWeatherUnits(val refresh: Boolean): SettingsIntent()

    data class ManageSetLocationSheet(val show: Boolean): SettingsIntent()
    data class SetLocation(val location: String): SettingsIntent()
    data object SetCurrLocationAsDefault: SettingsIntent()

    data object SignOut: SettingsIntent()
}