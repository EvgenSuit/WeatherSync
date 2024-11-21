package com.weathersync.utils.ai.gemini.data

import com.weathersync.utils.ai.data.AISuggestionsProperties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double,
    val maxOutputTokens: Int?,
    val topP: Double,
    val topK: Int,
    @SerialName("response_mime_type")
    val responseMimeType: String,
    @SerialName("response_schema")
    val responseSchema: GeminiResponseSchema
)

@Serializable
data class GeminiResponseSchema(
    val type: String,
    val properties: AISuggestionsProperties
)

@Serializable
data class GeminiRequest(
    @SerialName("system_instruction")
    val systemInstruction: GeminiInput,
    val contents: GeminiInput,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)
@Serializable
data class GeminiCandidate(
    val content: GeminiParts
)

@Serializable
data class GeminiInput(
    val parts: GeminiPart
)
@Serializable
data class GeminiParts(
    val parts: List<GeminiPart>
)