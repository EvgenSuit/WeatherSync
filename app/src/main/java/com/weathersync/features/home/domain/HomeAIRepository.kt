package com.weathersync.features.home.domain

import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import com.weathersync.utils.ai.AIClientProvider
import com.weathersync.utils.ai.gemini.data.GenerationOptions
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.weather.weatherCodeToDescription
import kotlinx.serialization.json.Json
import java.util.Locale

class HomeAIRepository(
    private val aiClientProvider: AIClientProvider,
    private val currentWeatherDAO: CurrentWeatherDAO
) {
    suspend fun generateSuggestions(
        isLimitReached: Boolean,
        isSubscribed: IsSubscribed,
        currentWeather: CurrentWeather): Suggestions {
        if (isLimitReached) return currentWeatherDAO.getSuggestions() ?: Suggestions()
        val prompt = constructSuggestionsPrompt(currentWeather)
        val plainText = aiClientProvider.getAIClient(isSubscribed).generate(
            generationOptions = GenerationOptions(
                systemInstructions = systemInstructions,
                prompt = prompt,
                useStructuredOutput = true,
                maxOutputTokens = 2000,
                topP = 0.8,
                topK = 10
            )
        )
        val suggestions = Json.decodeFromString<Suggestions>(plainText)
        return suggestions
    }
    suspend fun insertSuggestions(suggestions: Suggestions) = currentWeatherDAO.insertSuggestions(suggestions)
    private fun constructSuggestionsPrompt(currentWeather: CurrentWeather) =
        """Temperature: ${currentWeather.temp} ${currentWeather.tempUnit},
            Wind speed: ${currentWeather.windSpeed} ${currentWeather.windSpeedUnit},
            Current local time: ${currentWeather.time}, locality: ${currentWeather.locality},
            Weather description: ${weatherCodeToDescription(currentWeather.weatherCode)}.
        """.trimMargin()

    private val systemInstructions =
        """Generate short not repeating suggestions based on weather data.
             Make sure to give a reason behind every suggestion.
            Give suggestions that don't contradict themselves (contradiction can happen when an activity is marked as recommended and unrecommended at the same time).  
            Don't make very obvious recommendations.
            Don't recommend 18+ activities (such as going to the bar).
            Use language: ${Locale.getDefault().language}.
            In the unrecommended activities use pronouns like don't or avoid, in recommended activities and what to wear/bring suggestions use encouraging pronouns.
            Every recommendation MUST be printed without numeration, *, #, dots at the end, with uppercase first letter.
        """.trimIndent()
}