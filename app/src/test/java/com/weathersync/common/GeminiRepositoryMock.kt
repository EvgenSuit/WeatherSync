package com.weathersync.common

import com.weathersync.features.home.GeminiRepository
import com.weathersync.features.home.recommendedActivitiesTag
import com.weathersync.features.home.unrecommendedActivitiesTag
import com.weathersync.features.home.whatToBringTag
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

data class TestSuggestions(
    val recommendedActivities: List<String> = listOf("Go for a walk in the park", "Ride a bicycle"),
    val unrecommendedActivities: List<String> = listOf("Avoid swimming outdoors for prolonged periods of time", "Avoid eating ice cream"),
    val whatToBring: List<String> = listOf("Light shoes", "A hat")
)
val testSuggestions = TestSuggestions()
val generatedSuggestions = """
    $recommendedActivitiesTag 
    ${testSuggestions.recommendedActivities[0]}
    ${testSuggestions.recommendedActivities[1]}
    $recommendedActivitiesTag
    $unrecommendedActivitiesTag
    ${testSuggestions.unrecommendedActivities[0]}
    ${testSuggestions.unrecommendedActivities[1]}
    $unrecommendedActivitiesTag
    $whatToBringTag     
    ${testSuggestions.whatToBring[0]}
    ${testSuggestions.whatToBring[1]}   
    $whatToBringTag
""".trimIndent()
fun mockGeminiRepository(
    generatedContent: String? = null,
    suggestionsGenerationException: Exception? = null
): GeminiRepository =
    GeminiRepository(
        generativeModel = mockk {
            coEvery { generateContent(any<String>()) } answers {
                if (suggestionsGenerationException != null) throw suggestionsGenerationException
                else mockk {
                    every { text } returns (generatedContent ?: generatedSuggestions)
                }
            }
        }
    )