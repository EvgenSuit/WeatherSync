package com.weathersync.common.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.userId
import com.weathersync.common.mockTask
import com.weathersync.features.home.WeatherUpdater
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import com.weathersync.utils.LimitManager
import com.weathersync.utils.LimitManagerConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.util.Date

fun mockLimitManager(
    limitManagerConfig: LimitManagerConfig,
    currentWeatherDAO: CurrentWeatherDAO,
    limitManagerFirestore: FirebaseFirestore,
    weatherUpdater: WeatherUpdater
) = LimitManager(
    limitManagerConfig = limitManagerConfig,
    auth = mockAuth(),
    firestore = limitManagerFirestore,
    currentWeatherDAO = currentWeatherDAO,
    weatherUpdater = weatherUpdater
)
fun mockLimitManagerFirestore(
    testClock: Clock,
    limitManagerConfig: LimitManagerConfig,
    timestamps: List<Timestamp>,
    serverTimestampGetException: Exception? = null,
    serverTimestampDeleteException: Exception? = null
): FirebaseFirestore = mockk {
    val docs = timestamps.map {
        mockk<DocumentSnapshot> {
            every { getTimestamp("timestamp") } returns it
        }
    }
    val someTimeAgo = slot<Timestamp>()
    val serverTimestamp = mockk<DocumentReference> {
        every { set(any()) } returns mockTask(
            taskException = serverTimestampGetException
        )
        every { delete() } returns mockTask(
            taskException = serverTimestampDeleteException
        )
        every { get() } returns mockTask(
            mockk {
                every { getTimestamp("timestamp") } returns Timestamp(testClock.instant())
            }
        )
    }
    every { batch() } returns mockk {
        every { delete(any()) } returns mockk()
        every { commit() } returns mockTask()
    }
    every { collection("serverTimestamp").document() } returns serverTimestamp
    every { collection(userId).document("limits").collection("currentWeatherLimits").whereLessThan("timestamp", capture(someTimeAgo)).get() } answers {
        mockTask(
            mockk {
                every { documents } returns docs.filter { it.getTimestamp("timestamp")!! < someTimeAgo.captured }
                    .map { mockk {
                        every { reference } returns mockk()
                    }
                    }
            }
        )
    }
    every { collection(userId).document("limits").collection("currentWeatherLimits").whereGreaterThanOrEqualTo("timestamp", capture(someTimeAgo)).count()
        .get(AggregateSource.SERVER) } answers {
        mockTask(
            mockk {
                every { count } returns docs.filter { it.getTimestamp("timestamp")!! >= someTimeAgo.captured }.size.toLong()
            }
        )
    }
    every { collection(userId).document("limits").collection("currentWeatherLimits").orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get() } returns mockTask(
        mockk {
            every { isEmpty } returns timestamps.isEmpty()
            every { documents } returns docs
        }
    )
    every { collection(userId).document("limits").collection("currentWeatherLimits")
        .add(any<Map<String, FieldValue>>()) } returns mockTask()
}

fun createDescendingTimestamps(
    limitManagerConfig: LimitManagerConfig,
    currTimeMillis: Long
) = List(limitManagerConfig.count+1) {
    Timestamp(Date(currTimeMillis + 60*1000 * it))
}.reversed()