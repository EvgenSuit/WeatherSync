package com.weathersync.features.activityPlanning.presentation

sealed class ActivityPlanningIntent {
    data object GenerateRecommendations: ActivityPlanningIntent()
    data class Input(val text: String): ActivityPlanningIntent()
}