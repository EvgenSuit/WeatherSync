package com.weathersync.features.home

import com.google.ai.client.generativeai.GenerativeModel
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.utils.AtLeastOneTagMissing
import java.util.Locale

val recommendedActivitiesTag = "[RECOMMENDED_ACTIVITIES]"
val unrecommendedActivitiesTag = "[UNRECOMMENDED_ACTIVITIES]"
val whatToBringTag = "[WHAT_TO_BRING]"
class GeminiRepository(
    private val generativeModel: GenerativeModel
) {
    suspend fun generateSuggestions(currentWeather: CurrentWeather): Suggestions {
        val prompt = constructSuggestionsPrompt(currentWeather)
        val plainText = generativeModel.generateContent(prompt).text ?: throw Exception("Empty response from Gemini")
        val extractedContent = extractContentWithTags(plainText,
            listOf(recommendedActivitiesTag, unrecommendedActivitiesTag, whatToBringTag))
        if (extractedContent.values.any { it.isEmpty() })
            throw AtLeastOneTagMissing("There's at least 1 tag missing in response from Gemini:" +
                    " Extracted content: $extractedContent. Prompt: $prompt. Plain response: $plainText")
        return Suggestions(
            recommendedActivities = extractedContent[recommendedActivitiesTag]!!.trim().split("\n"),
            unrecommendedActivities = extractedContent[unrecommendedActivitiesTag]!!.trim().split("\n"),
            whatToBring = extractedContent[whatToBringTag]!!.trim().split("\n")
        )
    }
    private fun extractContentWithTags(
        content: String,
        tags: List<String>
    ): Map<String, String> {
        val extractedContent = mutableMapOf<String, String>()
        tags.forEach { tag ->
            val escapedTag = Regex.escape(tag)
            val regex = Regex("(?<=${escapedTag})(.*?)(?=${escapedTag})", RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(content)?.value?.trim() ?: ""
            extractedContent[tag] = match.trim()
        }
        return extractedContent
    }
    private fun constructSuggestionsPrompt(currentWeather: CurrentWeather) =
        """Generate short suggestions on weather data:
            Temperature: ${currentWeather.temp} ${currentWeather.tempUnit},
            Wind speed: ${currentWeather.windSpeed} ${currentWeather.windSpeedUnit},
            Current local time: ${currentWeather.time}, locality: ${currentWeather.locality},
            WMO Weather interpretation code: ${currentWeather.weatherCode}.
        Follow this format: 
        $recommendedActivitiesTag[your recommendations go here]$recommendedActivitiesTag,
        $unrecommendedActivitiesTag[your recommendations go here]$unrecommendedActivitiesTag,
        and $whatToBringTag[your recommendations on what to wear/bring go here]$whatToBringTag.
        Don't make very obvious recommendations.
        Don't recommend 18+ activities (such as going to the bar).
        Use language: ${Locale.getDefault().language}.
        In the unrecommended activities use pronouns like don't or avoid, and in recommended activities use encouraging pronouns.
        Every recommendation must be printed in a list format split by \n without numeration, *, # and dots at the end.
        """.trimMargin()
}