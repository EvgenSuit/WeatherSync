package com.weathersync.common

import android.os.Bundle
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.weathersync.common.auth.userId
import com.weathersync.common.utils.mockAnalyticsManager
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.LimitManagerConfig
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
import java.util.Date
import java.util.Locale
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

    fun calculateNextUpdateDate(receivedNextUpdateDateTime: String?,
                                limitManagerConfig: LimitManagerConfig,
                                timestamps: List<Timestamp>,
                                locale: Locale): NextUpdateDate {
        val expectedNextUpdateDate = Date(timestamps.first().toDate().time + TimeUnit.HOURS.toMillis(limitManagerConfig.durationInHours.toLong()))

        // adjust time format (24-hour or AM/PM) based on locale
        val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, locale) as SimpleDateFormat
        val timePattern = timeFormatter.toPattern()
        val combinedPattern = "$timePattern, dd MMM"

        val combinedFormatter = SimpleDateFormat(combinedPattern, locale)
        val receivedNextUpdateDate = combinedFormatter.parse(receivedNextUpdateDateTime)

        return NextUpdateDate(expectedNextUpdateDate = expectedNextUpdateDate, receivedNextUpdateDate = receivedNextUpdateDate!!)
    }

    data class NextUpdateDate(val expectedNextUpdateDate: Date, val receivedNextUpdateDate: Date)
}