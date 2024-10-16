package com.weathersync.features.home

import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WeatherUpdater(
    private val clock: Clock,
    val minutes: Int
) {
    fun isLocalWeatherFresh(time: String): Boolean {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val currDate = LocalDateTime.now(clock)
        val formattedInputTime = LocalDateTime.parse(time, formatter)
        val duration = Duration.between(formattedInputTime, currDate)
        val isFresh = duration.toMinutes() < minutes
        return isFresh
    }
}