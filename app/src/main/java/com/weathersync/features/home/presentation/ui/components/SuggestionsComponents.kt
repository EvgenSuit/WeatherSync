package com.weathersync.features.home.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathersync.R
import com.weathersync.common.ui.CustomCircularProgressIndicator
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.ui.theme.ubuntuRegular

@Composable
fun RecommendedActivitiesComposable(
    recommendedActivities: List<String>?,
    unrecommendedActivities: List<String>?,
    isGenerationSuccessful: Boolean
) {
    CommonHomeComponent(titleRes = R.string.suggestions) {
        if (isGenerationSuccessful && !recommendedActivities.isNullOrEmpty() && !unrecommendedActivities.isNullOrEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                recommendedActivities.forEach { activity ->
                    ActivityComposable(activity = activity, recommended = true)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                unrecommendedActivities.forEach { activity ->
                    ActivityComposable(activity = activity, recommended = false)
                }
            }
        } else CustomCircularProgressIndicator(modifier = Modifier.testTag("SuggestionsProgress"))
    }
}

@Composable
fun WhatToWearComposable(recommendations: List<String>?,
                          isGenerationSuccessful: Boolean) {
    CommonHomeComponent(titleRes = R.string.what_to_wear_bring) {
        if (isGenerationSuccessful && !recommendations.isNullOrEmpty()) {
            recommendations.forEach { recommendation ->
                Text(text = "\u2022 $recommendation",
                    style = MaterialTheme.typography.displayMedium
                        .copy(fontSize = 21.sp))
                Box(Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center) {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(0.7f)
                        .padding(15.dp))
                }
            }
        } else CustomCircularProgressIndicator(modifier = Modifier.testTag("WhatToBringProgress"))
    }
}

@Composable
fun ActivityComposable(activity: String,
                       recommended: Boolean) {
    val baseColor = if (recommended) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val textColor = if (recommended) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onErrorContainer
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(baseColor.copy(0.4f))) {
        Text(text = activity,
            style = TextStyle(
                fontFamily = ubuntuRegular,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            ),
            modifier = Modifier.padding(14.dp)
        )

    }
}

@Preview
@Composable
fun SuggestionsPreview() {
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            RecommendedActivitiesComposable(
                recommendedActivities = listOf("Swimming", "Hiking", "Cycling"),
                unrecommendedActivities = listOf("Reading", "Painting", "Gardening"),
                isGenerationSuccessful = true
            )
        }
    }
}
@Preview
@Composable
fun WhatToWearPreview() {
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            WhatToWearComposable(
                recommendations = listOf("Jacket", "Shorts", "Umbrella"),
                isGenerationSuccessful = true
            )
        }
    }
}