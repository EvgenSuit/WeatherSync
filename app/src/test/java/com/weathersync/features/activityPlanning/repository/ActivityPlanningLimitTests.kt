package com.weathersync.features.activityPlanning.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.TestException
import com.weathersync.common.auth.userId
import com.weathersync.common.testInterfaces.BaseLimitTest
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.weather.FirestoreLimitCollection
import com.weathersync.utils.weather.GenerationType
import com.weathersync.utils.weather.Limit
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityPlanningLimitTests: BaseLimitTest {
    @get: Rule(order = 1)
    val activityPlanningBaseRule = ActivityPlanningBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(activityPlanningBaseRule.testDispatcher)

    @Test(expected = TestException::class)
    override fun getServerTimestamp_exception() = runTest {
        activityPlanningBaseRule.setupLimitManager(
            serverTimestampGetException = activityPlanningBaseRule.testHelper.testException)
        calculateLimit(isSubscribed = false)
    }

    @Test(expected = TestException::class)
    override fun deleteServerTimestamp_exception() = runTest {
        activityPlanningBaseRule.setupLimitManager(
            serverTimestampDeleteException = activityPlanningBaseRule.testHelper.testException)
        calculateLimit(isSubscribed = false)
    }

    @Test
    override fun limitReached_notSubscribed_isLimitCorrect() = runTest {
        calculateReachedLimit(
            isSubscribed = false,
            timestamps = createDescendingTimestamps(
                limitManagerConfig = activityPlanningBaseRule.regularLimitManagerConfig,
                currTimeMillis = activityPlanningBaseRule.testClock.millis())
        )
    }

    @Test
    override fun limitReached_isSubscribed_isLimitCorrect() = runTest {
        calculateReachedLimit(
            isSubscribed = true,
            timestamps = createDescendingTimestamps(
                limitManagerConfig = activityPlanningBaseRule.premiumLimitManagerConfig,
                currTimeMillis = activityPlanningBaseRule.testClock.millis())
        )
    }

    @Test
    override fun deleteOutdatedTimestamps_isSubscribed_success() = runTest {
        deleteOutdatedTimestampsUtil(isSubscribed = true)
    }


    @Test
    override fun deleteOutdatedTimestamps_notSubscribed_success() = runTest {
        deleteOutdatedTimestampsUtil(isSubscribed = false)
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

    private suspend fun deleteOutdatedTimestampsUtil(isSubscribed: IsSubscribed) {
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = activityPlanningBaseRule.regularLimitManagerConfig,
            currTimeMillis = activityPlanningBaseRule.testClock.millis())
        activityPlanningBaseRule.setupLimitManager(timestamps = timestamps)
        calculateLimit(isSubscribed = isSubscribed)

        activityPlanningBaseRule.testClock.advanceLimitBy(limitManagerConfig = activityPlanningBaseRule.regularLimitManagerConfig)
        activityPlanningBaseRule.setupLimitManager(timestamps = timestamps)
        calculateLimit(isSubscribed = isSubscribed)

        verifyOutdatedTimestampsDeletion(
            collection = FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS,
            testClock = activityPlanningBaseRule.testClock,
            timestamps = timestamps,
            limitManagerFirestore = activityPlanningBaseRule.limitManagerFirestore,
            limitManagerConfig = activityPlanningBaseRule.regularLimitManagerConfig
        )
    }

    override suspend fun calculateReachedLimit(
        isSubscribed: IsSubscribed,
        timestamps: List<Timestamp>
    ): Limit {
        activityPlanningBaseRule.setupLimitManager(timestamps = timestamps)
        val limit = calculateLimit(isSubscribed = isSubscribed)
        activityPlanningBaseRule.testHelper.assertNextUpdateTimeIsCorrect(
            receivedNextUpdateDateTime = limit.nextUpdateDateTime,
            limitManagerConfig = activityPlanningBaseRule.let {
                if (isSubscribed) it.premiumLimitManagerConfig else it.regularLimitManagerConfig
            },
            timestamps = timestamps)
        assertTrue(limit.isReached)
        coVerify { activityPlanningBaseRule.limitManager.calculateLimit(
            isSubscribed = isSubscribed,
            generationType = GenerationType.ActivityRecommendations) }
        return limit
    }

    override suspend fun calculateLimit(isSubscribed: IsSubscribed): Limit {
        activityPlanningBaseRule.setupActivityPlanningRepository(isSubscribed = isSubscribed)
        val limit = activityPlanningBaseRule.activityPlanningRepository.calculateLimit(isSubscribed)
        return limit
    }
}