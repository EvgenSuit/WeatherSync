package com.weathersync.features.home.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.weathersync.R
import com.weathersync.common.ui.CustomButton
import com.weathersync.ui.theme.WeatherSyncTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationRequester(
    onPermissionGranted: () -> Unit
) {
    val permissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_COARSE_LOCATION)
    var showLocationRequestDialog by rememberSaveable {
        mutableStateOf(false)
    }
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            showLocationRequestDialog = false
            onPermissionGranted()
        }
        else showLocationRequestDialog = true
    }

    if (showLocationRequestDialog) {
        LocationRequestDialog(onPermissionRequest = { permissionState.launchPermissionRequest() })
    }
}

@Composable
fun LocationRequestDialog(
    onPermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = { /*TODO*/ }) {
        Card(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .height(IntrinsicSize.Max)
                .padding(15.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp)
            ) {
                Text(text = stringResource(id = R.string.location_permission_required),
                    textAlign = TextAlign.Center)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CustomButton(
                        enabled = true,
                        text = stringResource(id = R.string.request_permission),
                        onClick = onPermissionRequest)
                    CustomButton(
                        enabled = true,
                        text = stringResource(id = R.string.go_to_settings),
                        onClick = { openAppSettings(context) })
                }
            }
        }
    }
}
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}

@Preview
@Composable
fun LocationRequestDialogPreview() {
    WeatherSyncTheme {
        Surface {
            LocationRequestDialog {

            }
        }
    }
}