package com.weathersync.utils.weather.limits

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Clock
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NextUpdateTimeFormatter(
    private val clock: Clock,
    private val locale: Locale
) {
    fun format(date: Date): String {
        val currDate = Date(clock.millis())
        val currCalendar = Calendar.getInstance(TimeZone.getTimeZone(clock.zone)).apply { time = currDate }
        val nextGenerationCalendar = Calendar.getInstance(TimeZone.getTimeZone(clock.zone)).apply { time = date }

        val timeFormat = (DateFormat.getTimeInstance(DateFormat.SHORT, locale))
        val timeString = timeFormat.format(date)

        val dateString = when {
            currCalendar.get(Calendar.DAY_OF_YEAR) != nextGenerationCalendar.get(Calendar.DAY_OF_YEAR) ->
                SimpleDateFormat("dd MMM", locale).format(date)
            currCalendar.get(Calendar.YEAR) != nextGenerationCalendar.get(Calendar.YEAR) ->
                SimpleDateFormat("dd MMM yyyy", locale).format(date)
            else -> null
        }
        return if (dateString != null) "$timeString, $dateString" else timeString
    }
}

