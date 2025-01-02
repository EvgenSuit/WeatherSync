package com.weathersync.utils.ai.openai

import com.weathersync.BuildConfig
import com.weathersync.utils.NullOpenAIResponse
import com.weathersync.utils.ai.AIClient
import com.weathersync.utils.ai.data.AISuggestionsProperties
import com.weathersync.utils.ai.data.ComplexResponseProperty
import com.weathersync.utils.ai.data.SimpleResponseProperty
import com.weathersync.utils.ai.data.GenerationOptions
import com.weathersync.utils.ai.openai.data.JsonSchema
import com.weathersync.utils.ai.openai.data.OpenAIMessage
import com.weathersync.utils.ai.openai.data.OpenAIRequest
import com.weathersync.utils.ai.openai.data.OpenAIResponse
import com.weathersync.utils.ai.openai.data.OpenAIResponseSchema
import com.weathersync.utils.ai.openai.data.ResponseFormat
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class OpenAIClient(
    engine: HttpClientEngine
): AIClient {
    private val httpClient = getHttpClient(
        engine = engine,
        defaultRequestUrl = "https://api.openai.com/v1/"
    )
   override suspend fun generate(generationOptions: GenerationOptions): String {
       val responseFormat = if (generationOptions.useStructuredOutput) {
           val openAiComplexResponseProperty = ComplexResponseProperty(
               type = "array",
               items = SimpleResponseProperty(type = "string")
           )
           ResponseFormat(
               type = "json_schema",
               jsonSchema = JsonSchema(
                   name = "suggestions_extraction",
                   schema = OpenAIResponseSchema(
                       type = "object",
                       properties = AISuggestionsProperties(
                           recommendedActivities = openAiComplexResponseProperty,
                           unrecommendedActivities = openAiComplexResponseProperty,
                           whatToBring = openAiComplexResponseProperty
                       ),
                       required = listOf("recommendedActivities", "unrecommendedActivities", "whatToBring"),
                       additionalProperties = false
                   ),
                   strict = true
               )
           )
       } else ResponseFormat(type = "text")
       val response = httpClient.post("chat/completions") {
           contentType(ContentType.Application.Json)
           headers {
               append(HttpHeaders.Authorization, "Bearer ${BuildConfig.OPENAI_API_KEY}")
           }
           setBody(
               OpenAIRequest(
                   model = "ft:gpt-4o-mini-2024-07-18:personal::AilsnaRk:ckpt-step-14",
                   messages = listOf(
                       OpenAIMessage(
                           role = "system",
                           content = generationOptions.systemInstructions
                       ),
                       OpenAIMessage(
                           role = "user",
                           content = generationOptions.prompt
                       )
                   ),
                   temperature = generationOptions.temperature,
                   topP = generationOptions.topP,
                   maxCompletionTokens = generationOptions.maxOutputTokens,
                   responseFormat = responseFormat
               )
           )
       }.body<OpenAIResponse>()
       val responseContent = response.choices.firstOrNull()?.message?.content?.replace("*", "")
       if (responseContent.isNullOrBlank() || responseContent == "null") throw NullOpenAIResponse("Prompt: ...${generationOptions.prompt.takeLast(500)}\n" +
               "Complete response: $response")
       return responseContent
   }
}