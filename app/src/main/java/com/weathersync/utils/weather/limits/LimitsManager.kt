package com.weathersync.utils.weather.limits

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.weathersync.features.home.domain.WeatherUpdater
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import com.weathersync.utils.TimeAPI
import com.weathersync.utils.subscription.IsSubscribed
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

sealed class QueryType(
    val regularLimitManagerConfig: LimitManagerConfig,
    val premiumLimitManagerConfig: LimitManagerConfig
) {
    data class CurrentWeather(val refresh: Boolean?): QueryType(
        regularLimitManagerConfig = LimitManagerConfig(2, 2),
        premiumLimitManagerConfig = LimitManagerConfig(4, 2)
    )
    data object ActivityRecommendations: QueryType(
        regularLimitManagerConfig = LimitManagerConfig(2, 6),
        premiumLimitManagerConfig = LimitManagerConfig(8, 6)
    )
    data object LocationSet: QueryType(
        regularLimitManagerConfig = LimitManagerConfig(0, 0),
        premiumLimitManagerConfig = LimitManagerConfig(4, 12)
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
    ACTIVITY_RECOMMENDATIONS_LIMITS("activityRecommendationsLimits"),
    LOCATION_SET_LIMITS("locationSetLimits")
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
                               queryType: QueryType
    ): Limit {
        // ignore local weather limits and fetch remote ones on refresh
        if (queryType is QueryType.CurrentWeather && queryType.refresh != null
            && !queryType.refresh) {
            val savedWeather = currentWeatherDAO.getWeather()
            val isLocalWeatherFresh = savedWeather?.time?.let { time ->
                weatherUpdater.isLocalWeatherFresh(time)
            } ?: false
            if (isLocalWeatherFresh) return Limit(isReached = true)
        }
        val realDateTime = timeAPI.getRealDateTime()
        val currentTime = Timestamp(realDateTime)
        val ref = when (queryType) {
            is QueryType.CurrentWeather -> limitsDoc.collection(FirestoreLimitCollection.CURRENT_WEATHER_LIMITS.collectionName)
            is QueryType.ActivityRecommendations -> limitsDoc.collection(FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS.collectionName)
            is QueryType.LocationSet -> limitsDoc.collection(FirestoreLimitCollection.LOCATION_SET_LIMITS.collectionName)
        }
        return ref.manageLimits(isSubscribed = isSubscribed,
            currentTime = currentTime,
            queryType = queryType)
    }
    private suspend fun CollectionReference.manageLimits(
        isSubscribed: IsSubscribed,
        currentTime: Timestamp,
        queryType: QueryType
    ): Limit {
        val limitManagerConfig = if (isSubscribed) queryType.premiumLimitManagerConfig
        else queryType.regularLimitManagerConfig

        val duration = TimeUnit.HOURS.toMillis(limitManagerConfig.durationInHours.toLong())
        val timeBefore = Date(currentTime.toDate().time - duration)

        val querySnapshot = this.orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()

        val timestampsAfter = querySnapshot.documents.filter { it.getTimestamp("timestamp")?.toDate()?.after(timeBefore) ?: false}
        val timestampsBefore = querySnapshot.documents.filter { it.getTimestamp("timestamp")?.toDate()?.before(timeBefore) ?: false }
        val countAfter = timestampsAfter.size

        deleteDocs(timestampsBefore)

        if (countAfter >= limitManagerConfig.count) {
            val lastTimestamp = timestampsAfter.firstOrNull()?.getTimestamp("timestamp")
                ?.toDate()?.time ?: return Limit(isReached = false)
            // add "durationInHours" hours to the last (most recent) timestamp
            val nextUpdateDateTime = Date(lastTimestamp + duration)
            return Limit(isReached = true, nextUpdateDateTime = nextUpdateDateTime)
        } else return Limit(isReached = false)
    }

    private suspend fun deleteDocs(docs: List<DocumentSnapshot>) {
        if (docs.isNotEmpty()) {
            val batch = firestore.batch()
            docs.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }
    }

    suspend fun recordTimestamp(queryType: QueryType) {
        when (queryType) {
            is QueryType.CurrentWeather -> limitsDoc.collection(FirestoreLimitCollection.CURRENT_WEATHER_LIMITS.collectionName)
            is QueryType.ActivityRecommendations -> limitsDoc.collection(FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS.collectionName)
            is QueryType.LocationSet -> limitsDoc.collection(FirestoreLimitCollection.LOCATION_SET_LIMITS.collectionName)
        }.addTimestamp()
    }

    private suspend fun CollectionReference.addTimestamp() {
        this.add(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
    }
}