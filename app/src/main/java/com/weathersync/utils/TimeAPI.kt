package com.weathersync.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class TimeAPIResponse(
    val dateTime: String
)

class TimeAPI(
    engine: HttpClientEngine,
    private val locale: Locale = Locale.getDefault()
) {
    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(Logging)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        defaultRequest {
            url("https://timeapi.io/api/time/current/")
        }
    }

    /**
     * returns global UTC time
     */
    suspend fun getRealDateTime(): Date {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale)
        // set to UTC since timeZone returns the value of device's timezone by default
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val stringDateTime = httpClient.get("zone?timeZone=Etc%2FUTC").body<TimeAPIResponse>().dateTime
        return sdf.parse(stringDateTime)!!
    }
}