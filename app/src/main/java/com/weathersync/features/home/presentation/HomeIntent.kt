package com.weathersync.features.home.presentation

sealed class HomeIntent {
    data object GetCurrentWeather: HomeIntent()
    data object RefreshCurrentWeather: HomeIntent()
}