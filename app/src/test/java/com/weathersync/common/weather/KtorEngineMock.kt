package com.weathersync.common.weather

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> mockEngine(
    status: HttpStatusCode,
    responseValue: T
): HttpClientEngine = MockEngine {
    val jsonResponse = Json.encodeToString(responseValue)
    respond(
        content = ByteReadChannel(jsonResponse),
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}