package com.weathersync.features.auth.presentation

import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.weathersync.R
import com.weathersync.common.ui.UIText
import com.weathersync.features.auth.domain.GoogleAuthRepository
import com.weathersync.features.auth.presentation.ui.AuthTextFieldsState
import com.weathersync.ui.AuthUIEvent
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.CustomResult
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.SignInWithGoogleActivityResultException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val googleAuthRepository: GoogleAuthRepository,
    private val analyticsManager: AnalyticsManager,
): ViewModel() {
    private val _uiState = MutableStateFlow(AuthUIState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AuthUIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.GoogleAuth -> signInWithGoogle(intent.result)
        }
    }

    suspend fun onTapSignIn(): IntentSender? =
        try {
            updateAuthResult(CustomResult.InProgress)
            googleAuthRepository.onTapSignIn()
        } catch (e: Exception) {
            _uiEvent.emit(AuthUIEvent.ShowSnackbar(UIText.StringResource(R.string.google_sign_in_error)))
            updateAuthResult(CustomResult.Error)
            analyticsManager.recordException(e)
            null
        }
    private fun signInWithGoogle(activityResult: ActivityResult) {
        viewModelScope.launch {
            try {
                if (activityResult.resultCode != Activity.RESULT_OK && activityResult.data == null){
                    analyticsManager.recordException(SignInWithGoogleActivityResultException("Result code: ${activityResult.resultCode}. Data: ${activityResult.data}"))
                    updateAuthResult(CustomResult.Error)
                    return@launch
                }
                googleAuthRepository.signInWithIntent(activityResult.data!!)
                analyticsManager.logEvent(event = FirebaseEvent.SIGN_IN_WITH_GOOGLE)
                _uiEvent.emit(AuthUIEvent.NavigateToHome)
                updateAuthResult(CustomResult.Success)
            } catch (e: Exception) {
                if ((e is ApiException && e.statusCode != 16) || e !is ApiException) {
                    _uiEvent.emit(AuthUIEvent.ShowSnackbar(UIText.StringResource(R.string.auth_error)))
                    analyticsManager.recordException(e)
                    updateAuthResult(CustomResult.Error)
                } else updateAuthResult(CustomResult.None)
            }
        }
    }
    private fun updateAuthResult(customResult: CustomResult) =
        _uiState.update { it.copy(authResult = customResult) }

}

data class AuthUIState(
    val fieldsState: AuthTextFieldsState = AuthTextFieldsState(),
    val authResult: CustomResult = CustomResult.None
)