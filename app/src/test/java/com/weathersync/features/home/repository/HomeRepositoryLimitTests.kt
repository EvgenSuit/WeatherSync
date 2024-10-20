package com.weathersync.features.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.weathersync.common.TestException
import com.weathersync.common.auth.userId
import com.weathersync.common.utils.BaseLimitTest
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.mockedWeather
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.utils.FirestoreLimitCollection
import com.weathersync.utils.GenerationType
import com.weathersync.utils.Limit
import com.weathersync.utils.LimitManagerConfig
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class HomeRepositoryLimitTests: BaseLimitTest {
    @get: Rule
    val homeBaseRule = HomeBaseRule()

    @Test(expected = TestException::class)
    override fun getServerTimestamp_exception() = runTest {
        homeBaseRule.setupLimitManager(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            serverTimestampGetException = homeBaseRule.exception)
        calculateLimit()
    }
    @Test(expected = TestException::class)
    override fun deleteServerTimestamp_exception() = runTest {
        homeBaseRule.setupLimitManager(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            serverTimestampDeleteException = homeBaseRule.exception)
        calculateLimit()
    }

    @Test
    override fun limitReached_isLimitCorrect() = runTest {
        calculateReachedLimit(timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()))
    }

    @Test
    override fun deleteOutdatedTimestamps_success() = runTest {
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis())
        homeBaseRule.setupLimitManager(
            timestamps = timestamps,
            limitManagerConfig = homeBaseRule.limitManagerConfig
        )
        calculateLimit()

        homeBaseRule.testClock.advanceLimitBy(limitManagerConfig = homeBaseRule.limitManagerConfig)
        homeBaseRule.setupLimitManager(
            timestamps = timestamps,
            limitManagerConfig = homeBaseRule.limitManagerConfig
        )
        calculateLimit()

        verifyOutdatedTimestampsDeletion(
            collection = FirestoreLimitCollection.CURRENT_WEATHER_LIMITS,
            testClock = homeBaseRule.testClock,
            timestamps = timestamps,
            limitManagerFirestore = homeBaseRule.limitManagerFirestore,
            limitManagerConfig = homeBaseRule.limitManagerConfig
        )
    }
    @Test
    fun localWeatherRecentlyInserted_localWeatherLimitReached() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        val firstLimit = calculateLimit()
        // since weather is null, weather updater will not be called during limit calculations
        coVerify(inverse = true) { homeBaseRule.weatherUpdater.isLocalWeatherFresh(any()) }
        assertFalse(firstLimit.isReached)

        dao.insertWeather(mockedWeather.toCurrentWeather())
        val secondLimit = calculateLimit()
        coVerify { homeBaseRule.weatherUpdater.isLocalWeatherFresh(any()) }
        assertTrue(secondLimit.isReached)
    }

    @Test
    fun localWeatherNotRecentlyInserted_localWeatherLimitNotReached() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        val firstLimit = calculateLimit()
        // since weather is null, weather updater will not be called during limit calculations
        coVerify(inverse = true) { homeBaseRule.weatherUpdater.isLocalWeatherFresh(any()) }
        assertFalse(firstLimit.isReached)

        val currTime = LocalDateTime.ofInstant(homeBaseRule.testClock.instant(), ZoneId.systemDefault())
        dao.insertWeather(mockedWeather.toCurrentWeather().copy(time = currTime.format(DateTimeFormatter.ISO_DATE_TIME)))
        homeBaseRule.testClock.advanceBy((homeBaseRule.weatherUpdater.minutes*60*1000).toLong())
        val secondLimit = calculateLimit()
        coVerify { homeBaseRule.weatherUpdater.isLocalWeatherFresh(any()) }
        assertFalse(secondLimit.isReached)
    }

    @Test
    override fun recordTimestamp_success() = runTest {
        homeBaseRule.homeRepository.recordTimestamp()
        val ref = homeBaseRule.limitManagerFirestore.collection(userId).document("limits").collection("currentWeatherLimits")
        coVerify {
            homeBaseRule.limitManager.recordTimestamp(GenerationType.CurrentWeather)
            ref.add(any<Map<String, FieldValue>>())
        }
    }

    override suspend fun calculateReachedLimit(
        timestamps: List<Timestamp>
    ): Limit {
        homeBaseRule.setupLimitManager(
            timestamps = timestamps,
            limitManagerConfig = homeBaseRule.limitManagerConfig
        )
        val limit = calculateLimit()
        val nextUpdateDate = homeBaseRule.testHelper.calculateNextUpdateDate(
            receivedNextUpdateDateTime = limit.formattedNextUpdateTime,
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            timestamps = timestamps)
        assertEquals(nextUpdateDate.expectedNextUpdateDate.time, nextUpdateDate.receivedNextUpdateDate.time)
        assertTrue(limit.isReached)
        return limit
    }

    override suspend fun calculateLimit(): Limit {
        homeBaseRule.setupHomeRepository()
        val limit = homeBaseRule.homeRepository.calculateLimit()
        coVerify { homeBaseRule.limitManager.calculateLimit(GenerationType.CurrentWeather) }
        return limit
    }
}