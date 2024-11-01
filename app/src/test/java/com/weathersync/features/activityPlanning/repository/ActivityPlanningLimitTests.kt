package com.weathersync.features.activityPlanning.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.weathersync.common.TestException
import com.weathersync.common.auth.userId
import com.weathersync.common.utils.BaseLimitTest
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.utils.FirestoreLimitCollection
import com.weathersync.utils.GenerationType
import com.weathersync.utils.Limit
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class ActivityPlanningLimitTests: BaseLimitTest {
    @get: Rule
    val activityPlanningBaseRule = ActivityPlanningBaseRule()

    @Test(expected = TestException::class)
    override fun getServerTimestamp_exception() = runTest {
        activityPlanningBaseRule.setupLimitManager(
            locale = Locale.US,
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig,
            serverTimestampGetException = activityPlanningBaseRule.exception)
        calculateLimit()
    }

    @Test(expected = TestException::class)
    override fun deleteServerTimestamp_exception() = runTest {
        activityPlanningBaseRule.setupLimitManager(
            locale = Locale.US,
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig,
            serverTimestampDeleteException = activityPlanningBaseRule.exception)
        calculateLimit()
    }

    @Test
    override fun limitReached_UKLocale_isLimitCorrect() = runTest {
        calculateReachedLimit(
            locale = Locale.UK,
            timestamps = createDescendingTimestamps(
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig,
            currTimeMillis = activityPlanningBaseRule.testClock.millis())
        )
    }

    @Test
    override fun limitReached_USLocale_isLimitCorrect() = runTest {
        calculateReachedLimit(
            locale = Locale.US,
            timestamps = createDescendingTimestamps(
                limitManagerConfig = activityPlanningBaseRule.limitManagerConfig,
                currTimeMillis = activityPlanningBaseRule.testClock.millis())
        )
    }

    @Test
    override fun deleteOutdatedTimestamps_success() = runTest {
        val locale = Locale.US
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig,
            currTimeMillis = activityPlanningBaseRule.testClock.millis())
        activityPlanningBaseRule.setupLimitManager(
            locale = locale,
            timestamps = timestamps,
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig
        )
        calculateLimit()

        activityPlanningBaseRule.testClock.advanceLimitBy(limitManagerConfig = activityPlanningBaseRule.limitManagerConfig)
        activityPlanningBaseRule.setupLimitManager(
            locale = locale,
            timestamps = timestamps,
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig
        )
        calculateLimit()

        verifyOutdatedTimestampsDeletion(
            collection = FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS,
            testClock = activityPlanningBaseRule.testClock,
            timestamps = timestamps,
            limitManagerFirestore = activityPlanningBaseRule.limitManagerFirestore,
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig
        )
    }

    @Test
    override fun recordTimestamp_success() = runTest {
        activityPlanningBaseRule.activityPlanningRepository.recordTimestamp()
        val ref = activityPlanningBaseRule.limitManagerFirestore.collection(userId).document("limits")
            .collection(FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS.collectionName)
        coVerify {
            activityPlanningBaseRule.limitManager.recordTimestamp(GenerationType.ActivityRecommendations)
            ref.add(any<Map<String, FieldValue>>())
        }
    }

    override suspend fun calculateReachedLimit(
        timestamps: List<Timestamp>, locale: Locale
    ): Limit {
        activityPlanningBaseRule.setupLimitManager(
            locale = locale,
            timestamps = timestamps,
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig
        )
        val limit = calculateLimit()
        val nextUpdateDate = activityPlanningBaseRule.testHelper.calculateNextUpdateDate(
            receivedNextUpdateDateTime = limit.formattedNextUpdateTime,
            limitManagerConfig = activityPlanningBaseRule.limitManagerConfig,
            timestamps = timestamps,
            locale = locale,)
        assertEquals(nextUpdateDate.expectedNextUpdateDate.time, nextUpdateDate.receivedNextUpdateDate.time)
        assertTrue(limit.isReached)
        coVerify { activityPlanningBaseRule.limitManager.calculateLimit(GenerationType.ActivityRecommendations) }
        return limit
    }

    override suspend fun calculateLimit(): Limit {
        activityPlanningBaseRule.setupActivityPlanningRepository()
        return activityPlanningBaseRule.activityPlanningRepository.calculateLimit()
    }
}