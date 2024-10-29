package com.weathersync.common

import com.google.firebase.Timestamp
import com.weathersync.common.utils.mockCrashlyticsManager
import com.weathersync.utils.LimitManagerConfig
import io.mockk.slot
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

class TestException(message: String) : Exception(message)

class TestClock : Clock() {
    private var currInstant = Instant.ofEpochSecond(12 * 24 * 60 * 60)

    override fun instant(): Instant = currInstant

    override fun withZone(zone: ZoneId?): Clock {
        return this
    }

    override fun getZone(): ZoneId = ZoneId.systemDefault()

    fun advanceBy(durationMillis: Long) {
        currInstant = currInstant.plusMillis(durationMillis)
    }

    fun setInstant(instant: Instant) {
        currInstant = instant
    }
    // advance by durationInHours. test clock is used to calculate current server timestamp during mocking
    // (add 1 millisecond since whereLessThan query is used, at least for now)
    fun advanceLimitBy(limitManagerConfig: LimitManagerConfig) = advanceBy(Duration.ofHours(limitManagerConfig.durationInHours.toLong()+1).toMillis())
}

class TestHelper {
    val crashlyticsExceptionSlot = slot<Exception>()
    val testException = TestException("exception")
    val crashlyticsManager = mockCrashlyticsManager(exceptionSlot = crashlyticsExceptionSlot)

    fun calculateNextUpdateDate(receivedNextUpdateDateTime: String?,
                                limitManagerConfig: LimitManagerConfig,
                                timestamps: List<Timestamp>): NextUpdateDate {
        // in descending collection first timestamp is the most recent one
        val expectedNextUpdateDate = Date(timestamps.first().toDate().time + TimeUnit.HOURS.toMillis(limitManagerConfig.durationInHours.toLong()))
        val receivedNextUpdateDate = SimpleDateFormat("HH:mm, dd MMM").parse(receivedNextUpdateDateTime!!)!!
        return NextUpdateDate(expectedNextUpdateDate = expectedNextUpdateDate, receivedNextUpdateDate = receivedNextUpdateDate)
    }

    data class NextUpdateDate(val expectedNextUpdateDate: Date, val receivedNextUpdateDate: Date)
}