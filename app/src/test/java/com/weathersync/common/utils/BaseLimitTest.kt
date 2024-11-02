package com.weathersync.common.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestClock
import com.weathersync.common.TestException
import com.weathersync.common.auth.userId
import com.weathersync.utils.FirestoreLimitCollection
import com.weathersync.utils.Limit
import com.weathersync.utils.LimitManagerConfig
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

interface BaseLimitTest {
    @Test(expected = TestException::class)
    fun getServerTimestamp_exception()

    @Test(expected = TestException::class)
    fun deleteServerTimestamp_exception()

    @Test
    fun limitReached_UKLocale_isLimitCorrect()
    @Test
    fun limitReached_USLocale_isLimitCorrect()

    @Test
    fun deleteOutdatedTimestamps_success()

    @Test
    fun recordTimestamp_success()

    suspend fun calculateReachedLimit(timestamps: List<Timestamp>, locale: Locale): Limit
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