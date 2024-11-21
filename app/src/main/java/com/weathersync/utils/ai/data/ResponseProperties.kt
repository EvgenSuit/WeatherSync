package com.weathersync.utils.ai.data

import kotlinx.serialization.Serializable

@Serializable
data class AISuggestionsProperties(
    val recommendedActivities: ComplexResponseProperty,
    val unrecommendedActivities: ComplexResponseProperty,
    val whatToBring: ComplexResponseProperty
)

@Serializable
data class SimpleResponseProperty(
    val type: String
)
@Serializable
data class ComplexResponseProperty(
    val type: String,
    val items: SimpleResponseProperty
)