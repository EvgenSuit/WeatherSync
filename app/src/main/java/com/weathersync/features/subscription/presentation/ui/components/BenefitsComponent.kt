package com.weathersync.features.subscription.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathersync.R
import com.weathersync.ui.theme.quicksandMedium

@Composable
fun BenefitsComponent() {
    val benefits = listOf(R.string.improved_recommendations,
        R.string.increased_activity_planning_days,
        R.string.increased_limits,
        R.string.set_location_benefit,
        R.string.ad_free_experience)
    val shape = RoundedCornerShape(20.dp)
    ElevatedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 15.dp
        ),
        //border = BorderStroke(1.dp, MaterialTheme.colorScheme.inverseSurface),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            benefits.forEach { benefit ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "\u2022",
                        fontSize = 30.sp,
                        color = Color.Yellow.copy(0.8f))
                    Text(text = stringResource(id = benefit),
                        style = TextStyle(fontFamily = quicksandMedium,
                            fontSize = 18.sp)
                    )
                }
            }
        }
    }
}