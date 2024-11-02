package com.weathersync.features.settings.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.weathersync.BuildConfig
import com.weathersync.R

@Composable
fun AppVersionComponent() {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(top = 15.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(text = stringResource(id = R.string.app_version),
           color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Text(text = BuildConfig.VERSION_NAME,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}