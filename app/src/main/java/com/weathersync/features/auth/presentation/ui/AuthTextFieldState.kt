package com.weathersync.features.auth.presentation.ui

import com.weathersync.common.ui.TextFieldState

data class AuthTextFieldState(
    val type: AuthFieldType = AuthFieldType.Email,
    val state: TextFieldState = TextFieldState(),
)
data class AuthTextFieldsState(
    val email: AuthTextFieldState = AuthTextFieldState(AuthFieldType.Email),
    val password: AuthTextFieldState = AuthTextFieldState(AuthFieldType.Password),
)

sealed class AuthFieldType {
    data object Email: AuthFieldType()
    data object Password: AuthFieldType()
}