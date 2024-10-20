package com.weathersync.features.auth.presentation.ui

import android.content.IntentSender
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.R
import com.weathersync.common.ui.CustomButton
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.UIEvent
import com.weathersync.common.ui.UIText
import com.weathersync.features.auth.presentation.AuthIntent
import com.weathersync.features.auth.presentation.AuthType
import com.weathersync.features.auth.presentation.AuthUIState
import com.weathersync.features.auth.presentation.AuthViewModel
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.CustomResult
import com.weathersync.utils.isInProgress
import com.weathersync.utils.isSuccess
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel = koinViewModel(),
               onNavigateToHome: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarController = LocalSnackbarController.current
    uiState.authResult.let { result ->
        LaunchedEffect(result) {
            if (result.isSuccess()) onNavigateToHome()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UIEvent.ShowSnackbar -> snackbarController.showSnackbar(event.message)
            }
        }
    }
    AuthScreenContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onGetIntentSender = viewModel::onTapSignIn)
}

@Composable
fun AuthScreenContent(
    uiState: AuthUIState,
    onIntent: (AuthIntent) -> Unit,
    onGetIntentSender: suspend () -> IntentSender?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppTitle()
        MainComponents(
            // add password reset result later
            enabled = listOf(uiState.authResult).all { !it.isInProgress() },
            authType = uiState.authType,
            fieldsState = uiState.fieldsState,
            onAuthIntent = onIntent,
            onGetIntentSender = onGetIntentSender)
    }
}

@Composable
fun MainComponents(
    enabled: Boolean,
    authType: AuthType,
    fieldsState: AuthTextFieldsState,
    onAuthIntent: (AuthIntent) -> Unit,
    onGetIntentSender: suspend () -> IntentSender?
) {
    val isInputValid = fieldsState.email.state.error == UIText.Empty && fieldsState.password.state.error == UIText.Empty
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(350.dp)
    ) {
        AuthFields(
            enabled = enabled,
            fieldsState = fieldsState,
            onInput = onAuthIntent)
        CustomButton(
            enabled = isInputValid && enabled,
            text = stringResource(id = when(authType) {
            AuthType.SignIn -> R.string.sign_in
            AuthType.SignUp -> R.string.sign_up
        }),
            onClick = { onAuthIntent(AuthIntent.ManualAuth) })
        SignInWithGoogle(
            enabled = enabled,
            authType = authType,
            onAuth = onAuthIntent,
            onGetIntentSender = onGetIntentSender
        )
        HorizontalDivider()
        ChangeAuthType(
            enabled = enabled,
            currType = authType,
            onTypeChange = {
            onAuthIntent(AuthIntent.ChangeAuthType(it))
        })
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
                    authType = AuthType.SignUp,
                    authResult = CustomResult.InProgress
                ),
                onIntent = {},
                onGetIntentSender = { null }
            )
        }
    }
}