package com.weathersync.features.activityPlanning.presentation

sealed class ActivityPlanningIntent {
    data object GenerateTimes: ActivityPlanningIntent()
    data class Input(val text: String): ActivityPlanningIntent()
}