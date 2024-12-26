package com.weathersync.ui

import com.weathersync.common.ui.UIText

sealed class UIEvent {
    data class ShowSnackbar(val message: UIText): UIEvent()
}
sealed class AuthUIEvent {
    data class ShowSnackbar(val message: UIText): AuthUIEvent()
    data object NavigateToHome: AuthUIEvent()
}
sealed class HomeUIEvent {
    data class ShowSnackbar(val message: UIText): HomeUIEvent()
    data object NavigateToPremium: HomeUIEvent()
}
sealed class ActivityPlanningUIEvent {
    data class ShowSnackbar(val message: UIText): ActivityPlanningUIEvent()
    data object NavigateToPremium: ActivityPlanningUIEvent()
}
sealed class SettingsUIEvent {
    data class ShowSnackbar(val message: UIText): SettingsUIEvent()
    data object SignOut: SettingsUIEvent()
}
sealed class SubscriptionUIEvent {
    data class ShowSnackbar(val message: UIText): SubscriptionUIEvent()
    data object NavigateUp: SubscriptionUIEvent()
}