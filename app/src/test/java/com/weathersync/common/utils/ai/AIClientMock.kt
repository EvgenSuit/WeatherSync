package com.weathersync.common.utils.ai

import com.weathersync.common.weather.mockEngine
import com.weathersync.utils.ai.AIClientProvider
import com.weathersync.utils.ai.gemini.GeminiClient
import com.weathersync.utils.ai.gemini.data.GeminiCandidate
import com.weathersync.utils.ai.gemini.data.GeminiPart
import com.weathersync.utils.ai.gemini.data.GeminiParts
import com.weathersync.utils.ai.gemini.data.GeminiResponse
import com.weathersync.utils.ai.openai.OpenAIClient
import com.weathersync.utils.ai.openai.data.OpenAIChoice
import com.weathersync.utils.ai.openai.data.OpenAIMessage
import com.weathersync.utils.ai.openai.data.OpenAIResponse
import io.ktor.http.HttpStatusCode
import io.mockk.spyk

fun mockAIClientProvider(
    statusCode: HttpStatusCode,
    responseValue: String,
) = spyk(AIClientProvider(
    openAIClient = spyk(OpenAIClient(
        engine = mockEngine(
            status = statusCode,
            responseValue = OpenAIResponse(
                choices = listOf(
                    OpenAIChoice(
                        message = OpenAIMessage(
                            role = "system",
                            content = responseValue
                        )
                    )
                )
            )
        )
    )),
    geminiClient = spyk(GeminiClient(
        engine = mockEngine(
            status = statusCode,
            responseValue = GeminiResponse(
                candidates = listOf(GeminiCandidate(
                    content = GeminiParts(
                        parts = listOf(
                            GeminiPart(text = responseValue)
                        )
                    )
                )
                )
            )
        )
    )))
)