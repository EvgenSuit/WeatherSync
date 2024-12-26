package com.weathersync.utils.ai.gemini

import com.weathersync.BuildConfig
import com.weathersync.utils.NullGeminiResponse
import com.weathersync.utils.ai.AIClient
import com.weathersync.utils.ai.data.AISuggestionsProperties
import com.weathersync.utils.ai.data.ComplexResponseProperty
import com.weathersync.utils.ai.data.SimpleResponseProperty
import com.weathersync.utils.ai.gemini.data.GeminiGenerationConfig
import com.weathersync.utils.ai.gemini.data.GeminiInput
import com.weathersync.utils.ai.gemini.data.GeminiPart
import com.weathersync.utils.ai.gemini.data.GeminiRequest
import com.weathersync.utils.ai.gemini.data.GeminiResponse
import com.weathersync.utils.ai.gemini.data.GeminiResponseSchema
import com.weathersync.utils.ai.data.GenerationOptions
import com.weathersync.utils.ai.gemini.data.SafetySetting
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GeminiClient(
    engine: HttpClientEngine
): AIClient {
    private val httpClient = getHttpClient(
        engine = engine,
        defaultRequestUrl = "https://generativelanguage.googleapis.com/v1beta/"
    )
    override suspend fun generate(generationOptions: GenerationOptions)
    : String {
        val generationConfig = if (generationOptions.useStructuredOutput) {
            val geminiComplexResponseProperty = ComplexResponseProperty(
                type = "ARRAY",
                items = SimpleResponseProperty(type = "STRING")
            )
            GeminiGenerationConfig(
                temperature = generationOptions.temperature,
                maxOutputTokens = generationOptions.maxOutputTokens,
                topP = generationOptions.topP,
                topK = generationOptions.topK,
                responseMimeType = "application/json",
                responseSchema = GeminiResponseSchema(
                    type = "OBJECT",
                    properties = AISuggestionsProperties(
                        recommendedActivities = geminiComplexResponseProperty,
                        unrecommendedActivities = geminiComplexResponseProperty,
                        whatToBring = geminiComplexResponseProperty
                    )
                )
            )
        } else null
        val response = httpClient.post("models/gemini-1.5-pro:generateContent?key=${BuildConfig.GEMINI_API_KEY}") {
            contentType(ContentType.Application.Json)
            setBody(
                GeminiRequest(
                    systemInstruction = GeminiInput(
                        parts = GeminiPart(generationOptions.systemInstructions)
                    ),
                    contents = GeminiInput(
                        parts = GeminiPart(text = generationOptions.prompt)
                    ),
                    generationConfig = generationConfig,
                    safetySettings = listOf(
                        SafetySetting(category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_ONLY_HIGH"),
                        SafetySetting(category = "HARM_CATEGORY_HATE_SPEECH", threshold = "BLOCK_ONLY_HIGH")
                    )
                )
            )
        }
        val responseText = response.body<GeminiResponse>().candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (responseText.isNullOrBlank() || responseText == "null") throw NullGeminiResponse(
            "Last parts of prompt: ${generationOptions.prompt.takeLast(200)}...\n" +
                    "Complete response: $response.")
        return responseText
    }
}