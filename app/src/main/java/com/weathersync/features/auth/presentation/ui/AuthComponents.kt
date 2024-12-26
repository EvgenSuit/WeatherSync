package com.weathersync.features.auth.presentation.ui

import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathersync.R
import com.weathersync.features.auth.presentation.AuthIntent
import com.weathersync.ui.theme.arapeyRegular
import kotlinx.coroutines.launch


@Composable
fun AppIcon() {
    Image(painter = painterResource(id = R.drawable.app_icon),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .size(120.dp),
        contentDescription = "App icon")
}

@Composable
fun AppTitle() {
    Text(text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.titleLarge)
}

@Composable
fun AppSubtitle() {
    Text(text = stringResource(id = R.string.app_subtitle),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
fun SignInWithGoogle(
    enabled: Boolean,
    onAuth: (AuthIntent) -> Unit,
    onGetIntentSender: suspend () -> IntentSender?
) {
    val scope = rememberCoroutineScope()
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
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
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Google Icon"
            )
            Text(
                text = stringResource(R.string.continue_with_google),
                style = TextStyle(fontFamily = arapeyRegular),
                fontSize = 21.sp
            )
        }
    }
}