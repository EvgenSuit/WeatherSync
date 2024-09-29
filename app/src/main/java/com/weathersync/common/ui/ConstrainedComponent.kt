package com.weathersync.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.weathersync.R

@Composable
fun ConstrainedComponent(
    content: @Composable () -> Unit
) {
    val maxAllowedWidth = dimensionResource(id = R.dimen.max_width)
    BoxWithConstraints {
        Box(
            modifier = if (maxWidth > dimensionResource(id = R.dimen.max_width)) {
                Modifier.width(maxAllowedWidth)
            } else {
                Modifier.fillMaxWidth()
            }
        ) {
            content()
        }
    }
}