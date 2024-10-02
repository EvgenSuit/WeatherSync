package com.weathersync.features.home

import com.google.ai.client.generativeai.GenerativeModel
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.AtLeastOneTagMissing
import com.weathersync.utils.GeminiRepository
import java.util.Locale

val recommendedActivitiesTag = "[RECOMMENDED_ACTIVITIES]"
val unrecommendedActivitiesTag = "[UNRECOMMENDED_ACTIVITIES]"
val whatToBringTag = "[WHAT_TO_BRING]"
class GeminiRepository(
    private val generativeModel: GenerativeModel
): GeminiRepository {
    suspend fun generateSuggestions(currentWeather: CurrentWeather): Suggestions {
        val prompt = constructSuggestionsPrompt(currentWeather)
        val plainText = generativeModel.generateContent(prompt).text ?: throw Exception("Empty response from Gemini \n" +
                "Prompt: $prompt")
        val extractedContent = extractContentWithTags(
            prompt = prompt,
            content = plainText,
            tags = listOf(recommendedActivitiesTag, unrecommendedActivitiesTag, whatToBringTag))
        return Suggestions(
            recommendedActivities = extractedContent[0],
            unrecommendedActivities = extractedContent[1],
            whatToBring = extractedContent[2]
        )
    }
    private fun constructSuggestionsPrompt(currentWeather: CurrentWeather) =
        """Generate short suggestions based on weather data:
            Temperature: ${currentWeather.temp} ${currentWeather.tempUnit},
            Wind speed: ${currentWeather.windSpeed} ${currentWeather.windSpeedUnit},
            Current local time: ${currentWeather.time}, locality: ${currentWeather.locality},
            WMO Weather interpretation code: ${currentWeather.weatherCode}.
        You MUST follow this format without any deviations: 
        $recommendedActivitiesTag[your recommendations go here]$recommendedActivitiesTag,
        $unrecommendedActivitiesTag[your recommendations go here]$unrecommendedActivitiesTag,
        and $whatToBringTag[your recommendations on what to wear/bring go here]$whatToBringTag.
        Don't make very obvious recommendations.
        Don't recommend 18+ activities (such as going to the bar).
        Use language: ${Locale.getDefault().language}.
        In the unrecommended activities use pronouns like don't or avoid, and in recommended activities use encouraging pronouns.
        Every recommendation MUST be printed in a list format split by new paragraph without numeration, *, # and dots at the end.
        """.trimMargin()
}