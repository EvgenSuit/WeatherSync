package com.weathersync.common.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.userId
import com.weathersync.common.mockTask
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import com.weathersync.features.home.domain.WeatherUpdater
import com.weathersync.utils.TimeAPI
import com.weathersync.utils.weather.limits.FirestoreLimitCollection
import com.weathersync.utils.weather.limits.LimitManager
import com.weathersync.utils.weather.limits.LimitManagerConfig
import io.mockk.every
import io.mockk.mockk
import java.util.Date

fun mockLimitManager(
    currentWeatherDAO: CurrentWeatherDAO,
    limitManagerFirestore: FirebaseFirestore,
    weatherUpdater: WeatherUpdater,
    timeAPI: TimeAPI
) = LimitManager(
    auth = mockAuth(),
    firestore = limitManagerFirestore,
    currentWeatherDAO = currentWeatherDAO,
    weatherUpdater = weatherUpdater,
    timeAPI = timeAPI
)
fun mockLimitManagerFirestore(
    timestamps: List<Timestamp>,
    exception: Exception?
): FirebaseFirestore = mockk {
    val docs = timestamps.map {
        mockk<DocumentSnapshot> {
            every { getTimestamp("timestamp") } returns it
            every { reference } returns mockk()
        }
    }
    every { batch() } returns mockk {
        every { delete(any()) } returns mockk()
        every { commit() } returns mockTask(taskException = exception)
    }
    for (coll in listOf(
        FirestoreLimitCollection.CURRENT_WEATHER_LIMITS.collectionName,
        FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS.collectionName)) {
        every { collection(userId).document("limits").collection(coll)
            .orderBy("timestamp", Query.Direction.DESCENDING).get() } answers {
            mockTask(
                mockk {
                    every { documents } returns docs
                }, taskException = exception
            )
        }
        every { collection(userId).document("limits").collection(coll)
            .add(any<Map<String, FieldValue>>()) } returns mockTask(taskException = exception)
    }
}

fun createDescendingTimestamps(
    limitManagerConfig: LimitManagerConfig,
    currTimeMillis: Long
) = List(limitManagerConfig.count+1) {
    Timestamp(Date(currTimeMillis + 60*1000 * it))
}.reversed()