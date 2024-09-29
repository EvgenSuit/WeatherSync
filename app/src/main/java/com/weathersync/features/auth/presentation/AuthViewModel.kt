package com.weathersync.features.auth.presentation

import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.weathersync.R
import com.weathersync.common.ui.UIEvent
import com.weathersync.common.ui.UIText
import com.weathersync.features.auth.EmailValidator
import com.weathersync.features.auth.GoogleAuthRepository
import com.weathersync.features.auth.PasswordValidator
import com.weathersync.features.auth.RegularAuthRepository
import com.weathersync.features.auth.presentation.ui.AuthFieldType
import com.weathersync.features.auth.presentation.ui.AuthTextFieldState
import com.weathersync.features.auth.presentation.ui.AuthTextFieldsState
import com.weathersync.utils.CrashlyticsManager
import com.weathersync.utils.CustomResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val regularAuthRepository: RegularAuthRepository,
    private val googleAuthRepository: GoogleAuthRepository,
    private val crashlyticsManager: CrashlyticsManager,
): ViewModel() {
    private val emailValidator = EmailValidator()
    private val passwordValidator = PasswordValidator()

    private val _uiState = MutableStateFlow(AuthUIState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.ChangeAuthType -> _uiState.value = _uiState.value.copy(authType = intent.type)
            is AuthIntent.AuthInput -> performAuthInput(intent.state)
            is AuthIntent.ManualAuth -> performManualAuth()
            is AuthIntent.GoogleAuth -> signInWithGoogle(intent.result)
        }
    }
    private fun performAuthInput(state: AuthTextFieldState) {
        val type = state.type
        val value = state.state.value
        val error = when (type) {
            AuthFieldType.Email -> emailValidator(value)
            AuthFieldType.Password -> passwordValidator(value)
        }
        _uiState.update { it.copy(
            fieldsState = it.fieldsState.copy(
                email = if (type == AuthFieldType.Email)
                    it.fieldsState.email.copy(state = it.fieldsState.email.state.copy(value = value, error = error))
                else it.fieldsState.email,
                password = if (type == AuthFieldType.Password)
                    it.fieldsState.password.copy(state = it.fieldsState.password.state.copy(value = value, error = error))
                else it.fieldsState.password
            )
            ) }
    }
    suspend fun onTapSignIn(): IntentSender? =
        try {
            updateAuthResult(CustomResult.InProgress)
            googleAuthRepository.onTapSignIn()
        } catch (e: Exception) {
            _uiEvent.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.google_sign_in_error)))
            updateAuthResult(CustomResult.Error)
            crashlyticsManager.recordException(e)
            null
        }
    private fun signInWithGoogle(activityResult: ActivityResult) {
        viewModelScope.launch {
            try {
                if (activityResult.resultCode != Activity.RESULT_OK && activityResult.data == null){
                    updateAuthResult(CustomResult.Error)
                    return@launch
                }
                googleAuthRepository.signInWithIntent(activityResult.data!!)
                updateAuthResult(CustomResult.Success)
            } catch (e: Exception) {
                if ((e is ApiException && e.statusCode != 16) || e !is ApiException) {
                    _uiEvent.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.auth_error)))
                    updateAuthResult(CustomResult.Error)
                } else updateAuthResult(CustomResult.None)
            }
        }
    }
    private fun performManualAuth() {
        updateAuthResult(CustomResult.InProgress)
        viewModelScope.launch {
            try {
                val email = _uiState.value.fieldsState.email.state.value
                val password = _uiState.value.fieldsState.password.state.value
                regularAuthRepository.apply {
                    if (_uiState.value.authType == AuthType.SignIn) signIn(email, password) else signUp(email, password)
                }
                updateAuthResult(CustomResult.Success)
            } catch (e: Exception) {
                updateAuthResult(CustomResult.Error)
                crashlyticsManager.recordException(e, "Auth type: ${_uiState.value.authType}")
                _uiEvent.emit(UIEvent.ShowSnackbar(UIText.StringResource(R.string.auth_error)))
            }
        }
    }
    private fun updateAuthResult(customResult: CustomResult) =
        _uiState.update { it.copy(authResult = customResult) }

}

data class AuthUIState(
    val fieldsState: AuthTextFieldsState = AuthTextFieldsState(),
    val authType: AuthType = AuthType.SignIn,
    val authResult: CustomResult = CustomResult.None
)
sealed class AuthType {
    data object SignIn: AuthType()
    data object SignUp: AuthType()
}