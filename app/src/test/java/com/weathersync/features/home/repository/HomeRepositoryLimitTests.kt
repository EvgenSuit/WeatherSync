package com.weathersync.features.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.TestException
import com.weathersync.common.auth.userId
import com.weathersync.common.testInterfaces.BaseLimitTest
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.weather.limits.FirestoreLimitCollection
import com.weathersync.utils.weather.limits.GenerationType
import com.weathersync.utils.weather.limits.Limit
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@RunWith(AndroidJUnit4::class)
class HomeRepositoryLimitTests: BaseLimitTest {
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(homeBaseRule.testDispatcher)

    @Test(expected = ResponseException::class)
    override fun calculateLimit_timeApiException() = runTest {
        homeBaseRule.setupLimitManager(timeApiStatusCode = HttpStatusCode.Forbidden)
        calculateLimit(isSubscribed = false)
    }

    @Test(expected = TestException::class)
    override fun calculateLimit_firestoreException() = runTest {
        homeBaseRule.setupLimitManager(exception = TestException("exception"))
        calculateLimit(isSubscribed = false)
    }

    @Test
    override fun limitReached_notSubscribed_isLimitCorrect() = runTest {
        calculateReachedLimit(
            isSubscribed = false,
            timestamps = createDescendingTimestamps(
                limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
                currTimeMillis = homeBaseRule.testClock.millis()))
    }
    @Test
    override fun limitReached_isSubscribed_isLimitCorrect() = runTest {
        calculateReachedLimit(
            isSubscribed = true,
            timestamps = createDescendingTimestamps(
                limitManagerConfig = homeBaseRule.premiumLimitManagerConfig,
                currTimeMillis = homeBaseRule.testClock.millis())
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
    fun localWeatherRecentlyInserted_localWeatherLimitReached() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        val firstLimit = calculateLimit(isSubscribed = true)
        // since weather is null, weather updater will not be called during limit calculations
        coVerify(inverse = true) { homeBaseRule.weatherUpdater.isLocalWeatherFresh(any()) }
        assertFalse(firstLimit.isReached)

        dao.insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())
        val secondLimit = calculateLimit(isSubscribed = true)
        coVerify { homeBaseRule.weatherUpdater.isLocalWeatherFresh(any()) }
        assertTrue(secondLimit.isReached)
    }

    @Test
    fun localWeatherNotRecentlyInserted_localWeatherLimitNotReached() = runTest {
        localWeatherNotRecentlyInsertedUtility(isSubscribed = false)
    }
    @Test
    fun localWeatherNotRecentlyInserted_isSubscribed_localWeatherLimitNotReached() = runTest {
        localWeatherNotRecentlyInsertedUtility(isSubscribed = true)
    }

    @Test
    override fun recordTimestamp_success() = runTest {
        homeBaseRule.homeRepository.recordTimestamp()
        val ref = homeBaseRule.limitManagerFirestore.collection(userId).document("limits").collection(FirestoreLimitCollection.CURRENT_WEATHER_LIMITS.collectionName)
        coVerify {
            homeBaseRule.limitManager.recordTimestamp(GenerationType.CurrentWeather)
            ref.add(any<Map<String, FieldValue>>())
        }
    }

    private suspend fun deleteOutdatedTimestampsUtil(isSubscribed: IsSubscribed) {
        val limitManagerConfig = homeBaseRule.let {
            if (isSubscribed) it.premiumLimitManagerConfig else it.regularLimitManagerConfig
        }
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis())
        homeBaseRule.setupLimitManager(timestamps = timestamps)
        calculateLimit(isSubscribed = isSubscribed)

        homeBaseRule.testClock.advanceLimitBy(limitManagerConfig = limitManagerConfig)
        // delete outdated timestamps after advancing current clock (which is used to get server timestamp in tests)
        homeBaseRule.setupLimitManager(timestamps = timestamps)
        calculateLimit(isSubscribed = isSubscribed)

        verifyOutdatedTimestampsDeletion(
            collection = FirestoreLimitCollection.CURRENT_WEATHER_LIMITS,
            testClock = homeBaseRule.testClock,
            timestamps = timestamps,
            limitManagerFirestore = homeBaseRule.limitManagerFirestore,
            limitManagerConfig = limitManagerConfig
        )
    }

    private suspend fun localWeatherNotRecentlyInsertedUtility(isSubscribed: IsSubscribed) {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        val firstLimit = calculateLimit(isSubscribed)
        // since weather is null, weather updater will not be called during limit calculations
        coVerify(inverse = true) { homeBaseRule.weatherUpdater.isLocalWeatherFresh(any()) }
        assertFalse(firstLimit.isReached)

        val currTime = LocalDateTime.ofInstant(homeBaseRule.testClock.instant(), ZoneId.systemDefault())
        dao.insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather().copy(time = currTime.format(DateTimeFormatter.ISO_DATE_TIME)))
        homeBaseRule.testClock.advanceBy((homeBaseRule.weatherUpdater.minutes*60*1000).toLong())
        val secondLimit = calculateLimit(isSubscribed)
        coVerify { homeBaseRule.weatherUpdater.isLocalWeatherFresh(any()) }
        assertFalse(secondLimit.isReached)
    }

    override suspend fun calculateReachedLimit(
        isSubscribed: IsSubscribed,
        timestamps: List<Timestamp>
    ): Limit {
        homeBaseRule.setupLimitManager(timestamps = timestamps)
        val limit = calculateLimit(isSubscribed = isSubscribed)
        homeBaseRule.testHelper.assertNextUpdateTimeIsCorrect(
            receivedNextUpdateDateTime = limit.nextUpdateDateTime,
            limitManagerConfig = homeBaseRule.let { if (isSubscribed) it.premiumLimitManagerConfig else it.regularLimitManagerConfig },
            timestamps = timestamps)
        assertTrue(limit.isReached)
        return limit
    }

    override suspend fun calculateLimit(isSubscribed: IsSubscribed): Limit {
        homeBaseRule.setupHomeRepository(isSubscribed = isSubscribed)
        val limit = homeBaseRule.homeRepository.calculateLimit(isSubscribed)
        coVerify { homeBaseRule.limitManager.calculateLimit(
            isSubscribed = isSubscribed,
            generationType = GenerationType.CurrentWeather) }
        return limit
    }
}