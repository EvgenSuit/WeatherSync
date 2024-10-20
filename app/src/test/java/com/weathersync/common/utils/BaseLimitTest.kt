package com.weathersync.common.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestClock
import com.weathersync.common.TestException
import com.weathersync.common.auth.userId
import com.weathersync.features.home.mockedWeather
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.utils.FirestoreLimitCollection
import com.weathersync.utils.Limit
import com.weathersync.utils.LimitManagerConfig
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TestWatcher
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.TimeUnit

interface BaseLimitTest {
    @Test(expected = TestException::class)
    fun getServerTimestamp_exception()

    @Test(expected = TestException::class)
    fun deleteServerTimestamp_exception()

    @Test
    fun limitReached_isLimitCorrect()

    @Test
    fun deleteOutdatedTimestamps_success()

    @Test
    fun recordTimestamp_success()

    suspend fun calculateReachedLimit(timestamps: List<Timestamp>): Limit
    suspend fun calculateLimit(): Limit

    /**
     * verifies that **all** timestamps are outdated and deleted
     */
    suspend fun verifyOutdatedTimestampsDeletion(
        collection: FirestoreLimitCollection,
        testClock: TestClock,
        timestamps: List<Timestamp>,
        limitManagerFirestore: FirebaseFirestore,
        limitManagerConfig: LimitManagerConfig
    ) {
        val duration = TimeUnit.HOURS.toMillis(limitManagerConfig.durationInHours.toLong())
        val ref = limitManagerFirestore.collection(userId).document("limits")
            .collection(collection.collectionName)
            .whereLessThan("timestamp", Timestamp(Date(testClock.millis() - duration)))

        val batch = limitManagerFirestore.batch()
        verify(exactly = 1) { ref.get() }
        val docs = ref.get().await().documents
        assertEquals(timestamps.size, docs.size)
        verify(exactly = timestamps.size) { batch.delete(any()) }
    }
}