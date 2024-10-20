package com.weathersync.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.weathersync.R

@Composable
fun ConstrainedComponent(
    content: @Composable () -> Unit
) {
    val maxAllowedWidth = dimensionResource(id = R.dimen.max_width)
    BoxWithConstraints {
        val maxWidth = maxWidth
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // without verticalScroll pull to refresh will not work
        ) {
            Box(
                contentAlignment = Alignment.Center,
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
}