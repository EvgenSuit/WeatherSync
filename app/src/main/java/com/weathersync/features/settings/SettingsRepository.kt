package com.weathersync.features.settings

import com.weathersync.features.settings.data.Dark
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.WeatherUnitsManager

class SettingsRepository(
    private val themeManager: ThemeManager,
    private val weatherUnitsManager: WeatherUnitsManager
) {
    suspend fun setTheme(dark: Dark) = themeManager.setTheme(dark)
    fun themeFlow(isDarkByDefault: Dark) = themeManager.themeFlow(isDarkByDefault)

    suspend fun setWeatherUnit(unit: WeatherUnit) = weatherUnitsManager.setUnit(unit)
    suspend fun getUnits() = weatherUnitsManager.getUnits()
}