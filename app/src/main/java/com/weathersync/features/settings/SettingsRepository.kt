package com.weathersync.features.settings

import com.google.firebase.auth.FirebaseAuth
import com.weathersync.features.settings.data.Dark
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.weather.WeatherUnitsManager
import com.weathersync.utils.weather.limits.LimitManager
import com.weathersync.utils.weather.limits.QueryType
import com.weathersync.utils.weather.location.LocationManager

class SettingsRepository(
    private val auth: FirebaseAuth,
    private val themeManager: ThemeManager,
    private val weatherUnitsManager: WeatherUnitsManager,
    private val limitManager: LimitManager,
    private val subscriptionManager: SubscriptionManager,
    private val locationManager: LocationManager
) {
    suspend fun setTheme(dark: Dark) = themeManager.setTheme(dark)
    fun themeFlow(isDarkByDefault: Dark) = themeManager.themeFlow(isDarkByDefault)

    suspend fun isSubscribed() = subscriptionManager.initBillingClient()

    suspend fun setWeatherUnit(unit: WeatherUnit) = weatherUnitsManager.setUnit(unit)
    suspend fun getUnits() = weatherUnitsManager.getUnits()

    suspend fun calculateLocationSetLimits(isSubscribed: IsSubscribed) = limitManager.calculateLimit(isSubscribed = isSubscribed, queryType = QueryType.LocationSet)
    suspend fun incrementLocationSetLimits() = limitManager.recordTimestamp(queryType = QueryType.LocationSet)
    suspend fun setLocation(inputLocation: String): String = locationManager.setLocation(inputLocation)
    suspend fun setCurrLocationAsDefault(): String = locationManager.setCurrLocationAsDefault()

    fun signOut() = auth.signOut()
}