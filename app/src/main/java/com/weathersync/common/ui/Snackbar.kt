package com.weathersync.common.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.CustomResult
import com.weathersync.utils.isError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalSnackbarController = compositionLocalOf<SnackbarController> { error("No SnackbarController provided") }

class SnackbarController(
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope
) {
    // for testing purposes
    val hostState = snackbarHostState
    fun showSnackbar(message: UIText) {
        coroutineScope.launch {
            //snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }
}

@Composable
fun CustomSnackbar(
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val message = snackbarHostState.currentSnackbarData?.visuals?.message
    Snackbar(
        action = {
            val icon = Icons.Filled.Clear
            IconButton(onClick = onDismiss) {
                Icon(imageVector = icon,
                    contentDescription = icon.name)
            }
        },
        modifier = Modifier.padding(10.dp)
            .testTag("Snackbar")
    ) {
        message?.let {
            Text(text = it,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.testTag("Snackbar text: $it")) }
    }
}

sealed class UIEvent {
    data class ShowSnackbar(val message: UIText): UIEvent()
}

@Preview(showBackground = true)
@Composable
fun CustomSnackbarPreview() {
    WeatherSyncTheme(darkTheme = false) {
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("This is a preview message!")
        }
        Surface {
            CustomSnackbar(
                snackbarHostState = snackbarHostState,
                onDismiss = {}
            )
        }
    }
}