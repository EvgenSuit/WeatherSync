package com.weathersync.common.utils

import com.weathersync.common.weather.mockEngine
import com.weathersync.utils.TimeAPI
import com.weathersync.utils.TimeAPIResponse
import io.ktor.http.HttpStatusCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun mockTimeAPI(
    statusCode: HttpStatusCode,
    currTimeMillis: Long,
): TimeAPI {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val dateString = sdf.format(Date(currTimeMillis))
    return TimeAPI(
            engine = mockEngine(
                status = statusCode,
                responseValue = TimeAPIResponse(dateString)
            )
        )
    }