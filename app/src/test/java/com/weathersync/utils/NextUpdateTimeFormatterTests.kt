package com.weathersync.utils

import com.weathersync.common.TestClock
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class NextUpdateTimeFormatterTests {
    private val testClock = TestClock()
    private lateinit var nextUpdateTimeFormatter: NextUpdateTimeFormatter

    private fun setup(clock: Clock = testClock,
                      locale: Locale = Locale.US) {
        nextUpdateTimeFormatter = NextUpdateTimeFormatter(clock, locale)
    }

    @Before
    fun before() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun usLocale_sameDay() {
        testSameDay(locale = Locale.US)
    }

    @Test
    fun grLocale_sameDay() {
        testSameDay(locale = Locale.GERMANY)
    }

    @Test
    fun usLocale_differentDay() {
        testDifferentDay(locale = Locale.US)
    }

    @Test
    fun grLocale_differentDay() {
        testDifferentDay(locale = Locale.GERMANY)
    }

    @Test
    fun usLocale_differentYear() {
        testDifferentYear(locale = Locale.US)
    }

    @Test
    fun grLocale_differentYear() {
        testDifferentYear(locale = Locale.GERMANY)
    }

    @Test
    fun testDifferentTimeZone() {
        val clock = Clock.fixed(Instant.ofEpochMilli(30L*24*60*60*1000), ZoneId.of("Asia/Tokyo"))
        setup(
            locale = Locale.US,
            clock = clock)

        val targetDate = Date(clock.millis())
        val result = nextUpdateTimeFormatter.format(targetDate)
        val timeString = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(targetDate)

        println(result)
        kotlin.test.assertEquals(timeString, result)
    }


    private fun testSameDay(locale: Locale) {
        setup(locale = locale)
        val targetDate = Date(testClock.millis())

        val timeString = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(targetDate)

        val result = nextUpdateTimeFormatter.format(targetDate)
        println(result)
        kotlin.test.assertEquals(timeString, result)
    }
    private fun testDifferentDay(locale: Locale) {
        setup(locale = locale)
        val targetDate = Date(testClock.millis()+24L*60*60*1000)

        val timeString = "${DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(targetDate)}, " +
                SimpleDateFormat("dd MMM", locale).format(targetDate)

        val result = nextUpdateTimeFormatter.format(targetDate)
        println(result)
        kotlin.test.assertEquals(timeString, result)
    }
    private fun testDifferentYear(locale: Locale) {
        setup(locale = locale)
        val targetDate = Date(testClock.millis()+365L*24*60*60*1000)

        val timeString = "${DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(targetDate)}, " +
                SimpleDateFormat("dd MMM yyyy", locale).format(targetDate)

        val result = nextUpdateTimeFormatter.format(targetDate)
        println(result)
        kotlin.test.assertEquals(timeString, result)
    }
}
