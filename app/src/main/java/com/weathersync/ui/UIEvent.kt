package com.weathersync.ui

import com.weathersync.common.ui.UIText

sealed class UIEvent {
    data class ShowSnackbar(val message: UIText): UIEvent()
}
sealed class AuthUIEvent {
    data class ShowSnackbar(val message: UIText): AuthUIEvent()
    data object NavigateToHome: AuthUIEvent()
}