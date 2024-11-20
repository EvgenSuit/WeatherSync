package com.weathersync.utils.ai

import com.weathersync.utils.ai.gemini.GeminiClient
import com.weathersync.utils.ai.openai.OpenAIClient
import com.weathersync.utils.subscription.IsSubscribed
import io.ktor.client.engine.cio.CIO

class AIClientProvider(
    private val openAIClient: OpenAIClient,
    private val geminiClient: GeminiClient
) {
    fun getAIClient(isSubscribed: IsSubscribed) =
        if (isSubscribed) openAIClient else geminiClient
}