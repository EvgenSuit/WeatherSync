package com.weathersync.features.activityPlanning.presentation

sealed class ActivityPlanningIntent {
    data object RefreshLimits: ActivityPlanningIntent()
    data object GenerateRecommendations: ActivityPlanningIntent()
    data class Input(val text: String): ActivityPlanningIntent()
    data object NavigateToPremium: ActivityPlanningIntent()
}