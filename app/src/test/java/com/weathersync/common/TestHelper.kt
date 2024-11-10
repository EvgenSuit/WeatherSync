package com.weathersync.common

import android.os.Bundle
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.weathersync.common.auth.userId
import com.weathersync.common.utils.mockAnalyticsManager
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.weather.LimitManagerConfig
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TestException(message: String) : Exception(message)

// firebase timestamp is generated from this clock
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
    // advance by durationInHours to test outdated timestamps deletion. test clock is used to calculate current server timestamp during mocking
    // (add 1 millisecond since whereLessThan query is used, at least for now)
    fun advanceLimitBy(limitManagerConfig: LimitManagerConfig) = advanceBy(Duration.ofHours(limitManagerConfig.durationInHours.toLong()+1).toMillis())
}

class TestHelper {
    val exceptionSlot = slot<Exception>()
    val testException = TestException("exception")
    init {
        mockkStatic(FirebaseAnalytics::class)
    }

    val analytics: FirebaseAnalytics = mockk(relaxed = true)
    val analyticsManager = mockAnalyticsManager(
        exceptionSlot = exceptionSlot,
        analytics = analytics)

    fun verifyAnalyticsEvent(event: FirebaseEvent, inverse: Boolean, vararg params: Pair<String, String>) {
        // the list will always contain a single captured bundle, which is what i need. https://github.com/mockk/mockk/issues/352
        val bundles = mutableListOf<Bundle>()
        verify(inverse = inverse) { analytics.logEvent(event.name.lowercase(), capture(bundles)) }

        if (!inverse) {
            assertEquals(userId, bundles.last().getString("user_id"))

            params.forEach { param ->
                assertEquals(param.second, bundles.last().getString(param.first))
            }
        }
    }

    fun assertNextUpdateTimeIsCorrect(
        receivedNextUpdateDateTime: Date?,
        limitManagerConfig: LimitManagerConfig,
        timestamps: List<Timestamp>): NextUpdateDate {
        val expectedNextUpdateDate = Date.from(timestamps.first().toDate().toInstant().plus(
            Duration.ofHours(limitManagerConfig.durationInHours.toLong())
        ))
        assertEquals(expectedNextUpdateDate, receivedNextUpdateDateTime)
        return NextUpdateDate(expectedNextUpdateDate = expectedNextUpdateDate, receivedNextUpdateDate = receivedNextUpdateDateTime!!)
    }

    data class NextUpdateDate(val expectedNextUpdateDate: Date, val receivedNextUpdateDate: Date)
}