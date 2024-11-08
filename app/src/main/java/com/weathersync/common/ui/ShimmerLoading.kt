package com.weathersync.common.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun shimmerBrush(targetValue:Float = 1500f): Brush {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val shimmerColors = listOf(
        onBackground.copy(alpha = 0.6f),
        onBackground.copy(alpha = 0.3f),
        onBackground.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(1500), repeatMode = RepeatMode.Restart
        ), label = ""
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}
@Composable
fun ShimmerBox(height: Dp, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier.height(height)) {
        // Pass the width as the target value for the shimmer animation
        val targetValue = maxWidth.value * 10  // Adjust multiplier as needed for shimmer speed
        Box(modifier = Modifier.fillMaxSize().background(shimmerBrush(targetValue)))
    }
}