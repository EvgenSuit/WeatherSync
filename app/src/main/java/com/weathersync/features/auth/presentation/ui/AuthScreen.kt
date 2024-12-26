package com.weathersync.features.auth.presentation.ui

import android.content.IntentSender
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.PrivacyTermsLinks
import com.weathersync.features.auth.presentation.AuthIntent
import com.weathersync.features.auth.presentation.AuthUIState
import com.weathersync.features.auth.presentation.AuthViewModel
import com.weathersync.ui.AuthUIEvent
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.CustomResult
import com.weathersync.utils.isInProgress
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarController = LocalSnackbarController.current
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is AuthUIEvent.ShowSnackbar -> snackbarController.showSnackbar(event.message)
                is AuthUIEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }
    AuthScreenContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onGetIntentSender = viewModel::onTapSignIn
    )
}

@Composable
fun AuthScreenContent(
    uiState: AuthUIState,
    onIntent: (AuthIntent) -> Unit,
    onGetIntentSender: suspend () -> IntentSender?
) {
    ConstrainedComponent(
        spacedBy = 25.dp,
        modifier = Modifier
            .background(brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.background
                )
            ))
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        AppIcon()
        AppTitle()
        AppSubtitle()
        Spacer(modifier = Modifier.height(30.dp))
        SignInWithGoogle(
            enabled = !uiState.authResult.isInProgress(),
            onAuth = onIntent,
            onGetIntentSender = onGetIntentSender
        )
        PrivacyTermsLinks()
    }
}


@Preview//(device = "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240")
@Composable
fun AuthScreenPreview() {
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            AuthScreenContent(
                uiState = AuthUIState(
                    fieldsState = AuthTextFieldsState(),
                    authResult = CustomResult.None
                ),
                onIntent = {},
                onGetIntentSender = { null }
            )
        }
    }
}