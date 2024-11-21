package com.weathersync.utils

import com.weathersync.common.TestClock
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class NextUpdateTimeFormatterTests {
    private val testClock = TestClock()
    private lateinit var nextUpdateTimeFormatter: NextUpdateTimeFormatter

    private fun setup(clock: Clock, locale: Locale) {
        nextUpdateTimeFormatter = NextUpdateTimeFormatter(clock, locale)
    }

    @Before
    fun before() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun sameDay_UKLocale_isTimeCorrect() {
        testClock.setInstant(Instant.parse("2024-11-10T08:00:00Z"))
        setup(clock = testClock, locale = Locale.UK)

        val date = Date.from(Instant.parse("2024-11-10T15:00:00Z"))
        val formattedDate = nextUpdateTimeFormatter.formatNextUpdateDateTime(date)

        // Assuming UK uses 24-hour time format; it should display only the time
        assertEquals("15:00", formattedDate)
    }

    @Test
    fun differentDay_UKLocale_isDateTimeCorrect() {
        testClock.setInstant(Instant.parse("2024-11-10T08:00:00Z"))
        setup(clock = testClock, locale = Locale.UK)

        val date = Date.from(Instant.parse("2024-11-11T08:00:00Z"))
        val formattedDate = nextUpdateTimeFormatter.formatNextUpdateDateTime(date)

        // Should display time and date because it's a different day
        assertEquals("08:00, 11 Nov", formattedDate)
    }

    @Test
    fun differentYear_UKLocale_isFullDateTimeCorrect() {
        testClock.setInstant(Instant.parse("2024-12-31T23:59:59Z"))
        setup(clock = testClock, locale = Locale.UK)

        val date = Date.from(Instant.parse("2025-01-01T08:00:00Z"))
        val formattedDate = nextUpdateTimeFormatter.formatNextUpdateDateTime(date)

        // Should display time, day, month, and year because it's a different year
        assertEquals("08:00, 01 Jan, 2025", formattedDate)
    }

    @Test
    fun sameDay_USLocale_isTimeCorrect_AMPM() {
        testClock.setInstant(Instant.parse("2024-11-10T08:00:00Z"))
        setup(clock = testClock, locale = Locale.US)

        val date = Date.from(Instant.parse("2024-11-10T15:00:00Z"))
        val formattedDate = nextUpdateTimeFormatter.formatNextUpdateDateTime(date)

        // Assuming US uses 12-hour time format; should display only the time with AM/PM
        assertEquals("3:00 PM".removeWhitespaces(), formattedDate.removeWhitespaces())
    }

    @Test
    fun differentDay_USLocale_isTimeCorrect_AMPM() {
        testClock.setInstant(Instant.parse("2024-11-10T08:00:00Z"))
        setup(clock = testClock, locale = Locale.US)

        val date = Date.from(Instant.parse("2024-11-11T15:00:00Z"))
        val formattedDate = nextUpdateTimeFormatter.formatNextUpdateDateTime(date)

        // Assuming US uses 12-hour time format; should display only the time with AM/PM
        assertEquals("3:00 PM, 11 Nov".removeWhitespaces(), formattedDate.removeWhitespaces())
    }

    @Test
    fun differentYear_USLocale_isTimeCorrect_AMPM() {
        testClock.setInstant(Instant.parse("2024-11-10T08:00:00Z"))
        setup(clock = testClock, locale = Locale.US)

        val date = Date.from(Instant.parse("2025-11-11T15:00:00Z"))
        val formattedDate = nextUpdateTimeFormatter.formatNextUpdateDateTime(date)

        // Assuming US uses 12-hour time format; should display only the time with AM/PM
        assertEquals("3:00 PM, 11 Nov, 2025".removeWhitespaces(), formattedDate.removeWhitespaces())
    }

    @Test
    fun edgeCase_EndOfDayTransition() {
        testClock.setInstant(Instant.parse("2024-11-10T23:59:59Z"))
        setup(clock = testClock, locale = Locale.UK)

        val date = Date.from(Instant.parse("2024-11-11T00:00:00Z"))
        val formattedDate = nextUpdateTimeFormatter.formatNextUpdateDateTime(date)

        // Should display date and time because it's a different day
        assertEquals("00:00, 11 Nov", formattedDate)
    }

    private fun String.removeWhitespaces() = this
        .replace("\u202F", " ")
        .replace("\\s".toRegex(), "")
}
