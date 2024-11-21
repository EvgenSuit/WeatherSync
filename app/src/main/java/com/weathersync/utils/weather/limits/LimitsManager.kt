package com.weathersync.utils.weather.limits

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.weathersync.features.home.domain.WeatherUpdater
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import com.weathersync.utils.subscription.IsSubscribed
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

sealed class GenerationType(
    val regularLimitManagerConfig: LimitManagerConfig,
    val premiumLimitManagerConfig: LimitManagerConfig
) {
    data class CurrentWeather(val refresh: Boolean?): GenerationType(
        regularLimitManagerConfig = LimitManagerConfig(6, 6),
        premiumLimitManagerConfig = LimitManagerConfig(9, 6)
    )
    data object ActivityRecommendations: GenerationType(
        regularLimitManagerConfig = LimitManagerConfig(5, 6),
        premiumLimitManagerConfig = LimitManagerConfig(9, 6)
    )
}
data class Limit(
    val isReached: Boolean,
    val nextUpdateDateTime: Date? = null
)

/**
 * @param count specifies the max amount of times an operation can be performed within a certain time period (durationInHours)
 * @param durationInHours specifies the time period (from current time) within which the limit should be calculated
 */
data class LimitManagerConfig(
    val count: Int,
    val durationInHours: Int
)

enum class FirestoreLimitCollection(val collectionName: String) {
    CURRENT_WEATHER_LIMITS("currentWeatherLimits"),
    ACTIVITY_RECOMMENDATIONS_LIMITS("activityRecommendationsLimits")
}

class LimitManager(
    auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val currentWeatherDAO: CurrentWeatherDAO,
    private val weatherUpdater: WeatherUpdater,
    private val timeAPI: TimeAPI
) {
    private val limitsDoc = firestore.collection(auth.currentUser!!.uid).document("limits")

    suspend fun calculateLimit(isSubscribed: IsSubscribed,
                               generationType: GenerationType
    ): Limit {
        // ignore local weather limits and fetch remote ones on refresh
        if (generationType is GenerationType.CurrentWeather && generationType.refresh != null
            && !generationType.refresh) {
            val savedWeather = currentWeatherDAO.getWeather()
            val isLocalWeatherFresh = savedWeather?.time?.let { time ->
                weatherUpdater.isLocalWeatherFresh(time)
            } ?: false
            if (isLocalWeatherFresh) return Limit(isReached = true)
        }
        val realDateTime = timeAPI.getRealDateTime()
        val currentTime = Timestamp(realDateTime)
        val ref = when (generationType) {
            is GenerationType.CurrentWeather -> limitsDoc.collection(FirestoreLimitCollection.CURRENT_WEATHER_LIMITS.collectionName)
            is GenerationType.ActivityRecommendations -> limitsDoc.collection(FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS.collectionName)
        }
        return ref.manageLimits(isSubscribed = isSubscribed,
            currentTime = currentTime,
            generationType = generationType)
    }
    private suspend fun CollectionReference.manageLimits(
        isSubscribed: IsSubscribed,
        currentTime: Timestamp,
        generationType: GenerationType
    ): Limit {
        val limitManagerConfig = if (isSubscribed) generationType.premiumLimitManagerConfig
        else generationType.regularLimitManagerConfig

        val duration = TimeUnit.HOURS.toMillis(limitManagerConfig.durationInHours.toLong())
        val sixHoursBefore = Timestamp(Date(currentTime.toDate().time - duration))
        val docsBefore = this.whereLessThan("timestamp", sixHoursBefore)
        deleteDocs(docsBefore)

        // counts the number of timestamps over the last "durationInHours" hours
        val countAfter = this.whereGreaterThanOrEqualTo("timestamp", sixHoursBefore).count().get(AggregateSource.SERVER)
            .await().count.toInt()
        if (countAfter >= limitManagerConfig.count) {
            // use firstOrNull since currentWeatherLimits collection might be empty by the time lastTimestamp is returned
            val lastTimestamp = this.orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get()
                .await().documents.firstOrNull()?.getTimestamp("timestamp") ?: return Limit(isReached = false)
            // add "durationInHours" hours to the last timestamp
            val nextUpdateDateTime = Date(lastTimestamp.toDate().time + duration)
            return Limit(isReached = true, nextUpdateDateTime = nextUpdateDateTime)
        } else return Limit(isReached = false)
    }

    private suspend fun deleteDocs(query: Query) {
        val docs = query.get().await().documents
        if (docs.isNotEmpty()) {
            val batch = firestore.batch()
            docs.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    suspend fun recordTimestamp(generationType: GenerationType) {
        when (generationType) {
            is GenerationType.CurrentWeather -> limitsDoc.collection(FirestoreLimitCollection.CURRENT_WEATHER_LIMITS.collectionName).addTimestamp()
            is GenerationType.ActivityRecommendations -> limitsDoc.collection(FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS.collectionName).addTimestamp()
        }
    }

    private suspend fun CollectionReference.addTimestamp() {
        this.add(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
    }
}