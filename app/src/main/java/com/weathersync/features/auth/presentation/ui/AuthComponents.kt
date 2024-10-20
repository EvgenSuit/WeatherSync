package com.weathersync.features.auth.presentation.ui

import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathersync.R
import com.weathersync.features.auth.presentation.AuthIntent
import com.weathersync.features.auth.presentation.AuthType
import com.weathersync.ui.theme.arapeyRegular
import kotlinx.coroutines.launch

@Composable
fun AppTitle() {
    Text(text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 150.dp, bottom = 100.dp))
}

@Composable
fun AuthFields(
    enabled: Boolean,
    fieldsState: AuthTextFieldsState,
    onInput: (AuthIntent) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AuthTextField(
            enabled = enabled,
            fieldState = fieldsState.email,
            onInput = onInput)
        AuthTextField(
            enabled = enabled,
            fieldState = fieldsState.password,
            onInput = onInput)
    }
}

@Composable
fun AuthTextField(
    enabled: Boolean,
    fieldState: AuthTextFieldState,
    onInput: (AuthIntent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val type = fieldState.type
    val state = fieldState.state
    val error = state.error?.asString(context) ?: ""
    val placeholderText = when (type) {
        AuthFieldType.Email -> stringResource(id = R.string.email)
        AuthFieldType.Password -> stringResource(id = R.string.password)
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(value = state.value,
            isError = error.isNotEmpty(),
            maxLines = 1,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = if (type == AuthFieldType.Email) ImeAction.Next else ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(true) }),
            placeholder = { Text(text = placeholderText,
                modifier = Modifier.alpha(0.6f)) },
            onValueChange = {
                onInput(AuthIntent.AuthInput(fieldState.copy(state = fieldState.state.copy(value = it))))
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(placeholderText))
        if (error.isNotEmpty()) {
            Text(text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.testTag("$placeholderText error"))
        }
    }
}

@Composable
fun SignInWithGoogle(
    enabled: Boolean,
    authType: AuthType,
    onAuth: (AuthIntent) -> Unit,
    onGetIntentSender: suspend () -> IntentSender?
) {
    val scope = rememberCoroutineScope()
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult() ) { result ->
        onAuth(AuthIntent.GoogleAuth(result))
    }
    ElevatedButton(
        enabled = enabled,
        onClick = {
            scope.launch {
                val intentSender = onGetIntentSender()
                if (intentSender != null) {
                    val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                    signInLauncher.launch(intentSenderRequest)
                }
            }
        },
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_angle)),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(5.dp)
        ) {
            Image(painter = painterResource(id = R.drawable.google), contentDescription = "Google Icon")
            Text(text = stringResource(id = when (authType) {
                AuthType.SignIn -> R.string.sign_in_with_google
                AuthType.SignUp -> R.string.sign_up_with_google
            }),
                style = TextStyle(fontFamily = arapeyRegular),
                fontSize = 17.sp
            )
        }
    }
}

@Composable
fun ChangeAuthType(
    enabled: Boolean,
    currType: AuthType,
    onTypeChange: (AuthType) -> Unit
) {
    Text(text = stringResource(id = when(currType) {
        AuthType.SignIn -> R.string.do_not_have_an_account
        AuthType.SignUp -> R.string.have_an_account
    }))
    TextButton(
        enabled = enabled,
        onClick = { onTypeChange(
        when (currType) {
            AuthType.SignIn -> AuthType.SignUp
            AuthType.SignUp -> AuthType.SignIn
        }
    ) }) {
        Text(text = stringResource(
            id = when (currType) {
                AuthType.SignIn -> R.string.go_to_sign_up
                AuthType.SignUp -> R.string.go_to_sign_in
            }
        ))
    }
}