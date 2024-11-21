package com.weathersync.features.home.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class Suggestions(
    @PrimaryKey val uid: Int = 0,
    val recommendedActivities: List<String> = emptyList(),
    val unrecommendedActivities: List<String> = emptyList(),
    val whatToBring: List<String> = emptyList()
)
