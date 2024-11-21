package com.weathersync.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.weathersync.R

@Composable
fun ConstrainedComponent(
    isScrollEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxAllowedWidth = dimensionResource(id = R.dimen.max_width)
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val maxWidth = maxWidth
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (isScrollEnabled) Modifier.verticalScroll(rememberScrollState())
                    else Modifier) // without verticalScroll pull to refresh will not work)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = if (maxWidth > dimensionResource(id = R.dimen.max_width)) {
                        Modifier.width(maxAllowedWidth)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}