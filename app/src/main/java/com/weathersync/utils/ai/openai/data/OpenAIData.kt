package com.weathersync.utils.ai.openai.data

import com.weathersync.utils.ai.data.AISuggestionsProperties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIMessage(
    val role: String,
    val content: String,
)

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int,
    @SerialName("top_p")
    val topP: Double,
    @SerialName("response_format")
    val responseFormat: ResponseFormat?
)

@Serializable
data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)
@Serializable
data class OpenAIChoice(
    val message: OpenAIMessage
)

@Serializable
data class ResponseFormat(
    val type: String,
    @SerialName("json_schema")
    val jsonSchema: JsonSchema? = null
)

@Serializable
data class JsonSchema(
    val name: String,
    val schema: OpenAIResponseSchema,
    val strict: Boolean
)
@Serializable
data class OpenAIResponseSchema(
    val type: String,
    val properties: AISuggestionsProperties,
    val required: List<String>,
    val additionalProperties: Boolean
)