package com.weathersync.features.auth.presentation

import androidx.activity.result.ActivityResult
import com.weathersync.features.auth.presentation.ui.AuthTextFieldState

sealed class AuthIntent {
    data class GoogleAuth(val result: ActivityResult): AuthIntent()
}