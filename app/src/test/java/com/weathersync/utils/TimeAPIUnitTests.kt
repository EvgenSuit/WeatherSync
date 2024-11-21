package com.weathersync.utils

import com.weathersync.common.TestClock
import com.weathersync.common.utils.mockTimeAPI
import com.weathersync.utils.weather.limits.TimeAPI
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class TimeAPIUnitTests {
    private lateinit var timeApi: TimeAPI
    private val testClock = TestClock()

    private fun setup(statusCode: HttpStatusCode) {
        timeApi = mockTimeAPI(
            statusCode = statusCode,
            currTimeMillis = testClock.millis()
        )
    }

    @Test
    fun getTime_success() = runTest {
        setup(HttpStatusCode.OK)
        val receivedDate = timeApi.getRealDateTime()
        assertEquals(Date(testClock.millis()), receivedDate)
    }
    @Test(expected = ResponseException::class)
    fun getTime_failure() = runTest {
        setup(HttpStatusCode.Forbidden)
        timeApi.getRealDateTime()
    }
}