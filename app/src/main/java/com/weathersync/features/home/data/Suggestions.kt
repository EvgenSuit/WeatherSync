package com.weathersync.features.home.data

data class Suggestions(
    val recommendedActivities: List<String> = emptyList(),
    val unrecommendedActivities: List<String> = emptyList(),
    val whatToBring: List<String> = emptyList()
)