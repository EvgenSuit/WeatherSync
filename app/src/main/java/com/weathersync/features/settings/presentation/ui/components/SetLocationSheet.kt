package com.weathersync.features.settings.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.R
import com.weathersync.common.ui.CustomButton
import com.weathersync.common.ui.NextGenerationComponent
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.subscription.IsSubscribed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetLocationSheet(
    sheetState: SheetState,
    isGeocodingInProgress: Boolean,
    isSubscribed: IsSubscribed?,
    nextWorldwideSetTime: String?,
    onDismiss: () -> Unit,
    onSet: (String) -> Unit,
    onSetCurrentLocationAsDefault: () -> Unit
) {
    var location by remember {
        mutableStateOf("")
    }
    val focusRequester = remember {
        FocusRequester()
    }
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("SetLocationSheet")) {
        // don't use constrained component since ModalBottomSheet automatically adapts to screen width
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .padding(bottom = 15.dp)
        ) {
            if (isSubscribed == true) {
                nextWorldwideSetTime?.let {
                    // no upgrade prompt needed since only premium users can set default location to any worldwide location
                    NextGenerationComponent(nextUpdateTime =
                    stringResource(id = R.string.next_location_set_time, it)) {}
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("SetCustomLocationTextField"))
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                CustomButton(enabled = !isGeocodingInProgress && location.isNotBlank(),
                    text = stringResource(id = R.string.set_location_sheet),
                    onClick = { onSet(location) },
                    modifier = Modifier.testTag("SetCustomLocationButton"))
            }
            CustomButton(enabled = !isGeocodingInProgress,
                text = stringResource(id = R.string.set_current_location_as_default),
                onClick = onSetCurrentLocationAsDefault)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SetLocationSheetPreview() {
    WeatherSyncTheme {
        Surface {
            SetLocationSheet(
                sheetState = rememberStandardBottomSheetState(),
                isGeocodingInProgress = false,
                isSubscribed = true,
                nextWorldwideSetTime = "04:34",
                onDismiss = { /*TODO*/ },
                onSetCurrentLocationAsDefault = {},
                onSet = {})
        }
    }
}