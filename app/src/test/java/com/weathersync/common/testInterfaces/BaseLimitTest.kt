package com.weathersync.common.testInterfaces

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestClock
import com.weathersync.common.TestException
import com.weathersync.common.auth.userId
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.weather.FirestoreLimitCollection
import com.weathersync.utils.weather.Limit
import com.weathersync.utils.weather.LimitManagerConfig
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
    fun limitReached_notSubscribed_isLimitCorrect()
    @Test
    fun limitReached_isSubscribed_isLimitCorrect()

    @Test
    fun deleteOutdatedTimestamps_notSubscribed_success()
    @Test
    fun deleteOutdatedTimestamps_isSubscribed_success()

    @Test
    fun recordTimestamp_success()

    suspend fun calculateReachedLimit(
        isSubscribed: IsSubscribed,
        timestamps: List<Timestamp>): Limit
    suspend fun calculateLimit(isSubscribed: IsSubscribed): Limit

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