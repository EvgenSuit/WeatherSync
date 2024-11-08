package com.weathersync.utils.weather

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.weathersync.features.home.WeatherUpdater
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class GenerationType {
    data object CurrentWeather: GenerationType()
    data object ActivityRecommendations: GenerationType()
}
data class Limit(
    val isReached: Boolean,
    val formattedNextUpdateTime: String? = null
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
    private val limitManagerConfig: LimitManagerConfig,
    auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val currentWeatherDAO: CurrentWeatherDAO,
    private val weatherUpdater: WeatherUpdater,
    private val locale: Locale
) {
    private val limitsDoc = firestore.collection(auth.currentUser!!.uid).document("limits")
    private val currentWeatherLimitsRef = limitsDoc.collection(FirestoreLimitCollection.CURRENT_WEATHER_LIMITS.collectionName)
    private val activityRecommendationsLimitsRef = limitsDoc.collection(FirestoreLimitCollection.ACTIVITY_RECOMMENDATIONS_LIMITS.collectionName)

    suspend fun calculateLimit(generationType: GenerationType): Limit {
        val currentTime = getServerTimestamp()
        return when (generationType) {
            is GenerationType.CurrentWeather ->
                currentWeatherLimitsRef.manageLimits(currentTime, generationType)
            is GenerationType.ActivityRecommendations ->
                activityRecommendationsLimitsRef.manageLimits(currentTime, generationType)
        }
    }
    private suspend fun CollectionReference.manageLimits(
        currentTime: Timestamp,
        generationType: GenerationType
    ): Limit {
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
            return Limit(isReached = true, formattedNextUpdateTime = nextUpdateDateTime.formatNextUpdateDateTime(currentTime))
        } else if (generationType == GenerationType.CurrentWeather) {
            // if account limit is not yet reached, but the local instance of current weather is fresh, consider the limit reached
            val savedWeather = currentWeatherDAO.getWeather()
            val isLocalWeatherFresh = savedWeather?.time?.let { time ->
                weatherUpdater.isLocalWeatherFresh(time)
            } ?: false
            return Limit(isReached = isLocalWeatherFresh)
        } else return Limit(isReached = false)
    }

    private fun Date.formatNextUpdateDateTime(currentTime: Timestamp): String {
        val currentDate = currentTime.toDate()
        val currentDateCalendar = Calendar.getInstance().apply { time = currentDate }
        val nextUpdateDateCalendar = Calendar.getInstance().apply { time = this@formatNextUpdateDateTime }

        // adjust time format (24-hour or AM/PM) based on locale
        var dateTimePattern = (DateFormat.getTimeInstance(DateFormat.SHORT, locale) as SimpleDateFormat).toPattern()
        if (currentDateCalendar.get(Calendar.DAY_OF_MONTH) != nextUpdateDateCalendar.get(Calendar.DAY_OF_MONTH)) {
            dateTimePattern += ", dd MMM"
        }
        if (currentDateCalendar.get(Calendar.YEAR) != nextUpdateDateCalendar.get(Calendar.YEAR)) {
            dateTimePattern += ", yyyy"
        }
        val formatter = SimpleDateFormat(dateTimePattern, locale)
        return formatter.format(this)
    }


    private suspend fun deleteDocs(query: Query) {
        val batch = firestore.batch()
        query.get().await().documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    suspend fun recordTimestamp(generationType: GenerationType) {
        when (generationType) {
            is GenerationType.CurrentWeather -> currentWeatherLimitsRef.addTimestamp()
            is GenerationType.ActivityRecommendations -> activityRecommendationsLimitsRef.addTimestamp()
        }
    }

    private suspend fun CollectionReference.addTimestamp() {
        this.add(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
    }

    private suspend fun getServerTimestamp(): Timestamp {
        val timestampRef = firestore.collection("serverTimestamp").document()
        timestampRef.set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
        val timestamp = timestampRef.get().await().getTimestamp("timestamp")
        timestampRef.delete().await()
        return timestamp!!
    }
}