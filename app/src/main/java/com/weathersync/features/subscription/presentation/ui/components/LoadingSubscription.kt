package com.weathersync.features.subscription.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.weathersync.common.ui.ShimmerBox
import com.weathersync.common.ui.shimmerBrush

@Composable
fun LoadingSubscription() {
    val shimmerModifier = Modifier.clip(RoundedCornerShape(20.dp))
        .fillMaxWidth()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ShimmerBox(height = 240.dp, shimmerModifier)
        ShimmerBox(height = 85.dp, shimmerModifier)
        ShimmerBox(height = 50.dp, shimmerModifier)
    }
}