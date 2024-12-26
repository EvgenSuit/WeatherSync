package com.weathersync.features.settings.presentation.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weathersync.R
import com.weathersync.features.settings.data.Dark
import com.weathersync.ui.theme.WeatherSyncTheme


@Composable
fun ThemeSwitcher(
    darkTheme: Dark?,
    size: Dp = 60.dp,
    iconSize: Dp = size / 3,
    padding: Dp = 10.dp,
    borderWidth: Dp = 1.dp,
    parentShape: Shape = CircleShape,
    toggleShape: Shape = CircleShape,
    animationSpec: AnimationSpec<Dp> = tween(durationMillis = 300),
    onClick: () -> Unit
) {
    val offset by animateDpAsState(
        targetValue = if (darkTheme == true) size else 0.dp,
        animationSpec = animationSpec
    )
    Box(modifier = Modifier
        .width(size * 2)
        .clip(shape = parentShape)
        .clickable(enabled = darkTheme != null) { onClick() }
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .testTag("ThemeSwitcher")
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(x = offset)
                .padding(all = padding)
                .clip(shape = toggleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Row(
            modifier = Modifier
                .border(
                    border = BorderStroke(
                        width = borderWidth,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    shape = parentShape
                )
        ) {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(iconSize)
                        .semantics {
                            selected = darkTheme == false
                        },
                    painter = painterResource(id = R.drawable.light_mode),
                    contentDescription = "LightMode",
                    tint = if (darkTheme == true) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer,
                )
            }
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(iconSize)
                        .semantics {
                            selected = darkTheme == true
                        },
                    painter = painterResource(id = R.drawable.dark_mode),
                    contentDescription = "DarkMode",
                    tint = if (darkTheme == true) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview
@Composable
fun ThemeSwitchPreview() {
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            CommonSettingsComponent(textId = R.string.theme) {
                ThemeSwitcher(darkTheme = null) {

                }
            }
        }
    }
}