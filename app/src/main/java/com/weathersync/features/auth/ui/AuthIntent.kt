package com.weathersync.features.auth.ui

import androidx.activity.result.ActivityResult

sealed class AuthIntent {
    data class ChangeAuthType(val type: AuthType): AuthIntent()
    data class AuthInput(val state: AuthTextFieldState): AuthIntent()
    data object ManualAuth: AuthIntent()
    data class GoogleAuth(val result: ActivityResult): AuthIntent()
}