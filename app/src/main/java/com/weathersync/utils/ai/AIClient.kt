package com.weathersync.utils.ai

import com.weathersync.utils.ai.gemini.data.GenerationOptions
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface AIClient {

    suspend fun generate(generationOptions: GenerationOptions): String

    fun getHttpClient(
        engine: HttpClientEngine,
        defaultRequestUrl: String) = HttpClient(engine) {
            expectSuccess = true
            install(Logging)
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
            defaultRequest {
                url(defaultRequestUrl)
            }
        }
}