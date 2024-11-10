package com.weathersync.utils.weather

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Clock
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NextUpdateTimeFormatter(
    private val clock: Clock,
    private val locale: Locale
) {
    fun formatNextUpdateDateTime(date: Date): String {
        val currentDate = Date(clock.millis())
        val currentDateCalendar = Calendar.getInstance().apply { time = currentDate }
        val nextUpdateDateCalendar = Calendar.getInstance().apply { time = date }

        // adjust time format (24-hour or AM/PM) based on locale
        var dateTimePattern = (DateFormat.getTimeInstance(DateFormat.SHORT, locale) as SimpleDateFormat).toPattern()
        if (currentDateCalendar.get(Calendar.DAY_OF_MONTH) != nextUpdateDateCalendar.get(Calendar.DAY_OF_MONTH)) {
            dateTimePattern += ", dd MMM"
        }
        if (currentDateCalendar.get(Calendar.YEAR) != nextUpdateDateCalendar.get(Calendar.YEAR)) {
            dateTimePattern += ", yyyy"
        }
        val formatter = SimpleDateFormat(dateTimePattern, locale)
        return formatter.format(date)
    }
}

