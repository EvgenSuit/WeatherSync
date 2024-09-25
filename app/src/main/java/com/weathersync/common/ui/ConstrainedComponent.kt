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
    BoxWithConstraints {
        Box(
            modifier = Modifier.let {
                if (maxWidth > dimensionResource(id = R.dimen.max_width))
            it.fillMaxWidth() else it.width(maxWidth)
            }
        ) {
            content()
        }
    }
}