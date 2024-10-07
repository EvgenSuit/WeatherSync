package com.weathersync.utils

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.weathersync.features.home.WeatherUpdater
import com.weathersync.features.home.data.db.CurrentWeatherDAO
import kotlinx.coroutines.tasks.await
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
    val nextUpdateDateTime: String? = null
)

class LimitManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val currentWeatherDAO: CurrentWeatherDAO,
    private val weatherUpdater: WeatherUpdater
) {
    suspend fun calculateLimit(generationType: GenerationType): Limit {
        val limitsDoc = firestore.collection(auth.currentUser!!.uid).document("limits")
        val currentTime = getServerTimestamp()
        return when (generationType) {
            is GenerationType.CurrentWeather -> manageCurrentWeatherLimits(currentTime, limitsDoc)
            is GenerationType.ActivityRecommendations -> manageActivityRecommendationsLimits(currentTime, limitsDoc)
        }
    }
    private suspend fun manageCurrentWeatherLimits(
        currentTime: Timestamp,
        limitsDoc: DocumentReference): Limit {
        val ref = limitsDoc.collection("currentWeatherLimits")
        val sixHoursBefore = Timestamp(Date(currentTime.toDate().time - (TimeUnit.HOURS.toMillis(6))))
        val docsBefore = ref.whereLessThan("timestamp", sixHoursBefore)
        docsBefore.deleteDocs()

        // counts number of timestamps over the last 6 hours
        val countAfter = ref.whereGreaterThanOrEqualTo("timestamp", sixHoursBefore).count().get(AggregateSource.SERVER)
            .await().count
        if (countAfter >= 6L) {
            // use firstOrNull since currentWeatherLimits might be empty by the time lastTimestamp is returned
            val lastTimestamp = ref.orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get()
                .await().documents.firstOrNull()?.getTimestamp("timestamp") ?: return Limit(isReached = false)
            // add 6 hours to last timestamp
            val nextUpdateDateTime = Date(lastTimestamp.toDate().time + TimeUnit.HOURS.toMillis(6))
            return Limit(isReached = true, nextUpdateDateTime = nextUpdateDateTime.formatNextUpdateDateTime(currentTime))
        } else {
            val savedWeather = currentWeatherDAO.getWeather()
            val isLocalWeatherFresh = savedWeather?.time?.let { time ->
                weatherUpdater.isLocalWeatherFresh(false, time)
            } ?: false
            println("Is fresh: $isLocalWeatherFresh; is limit reached: false," +
                    "time: ${savedWeather?.time}")
            if (!isLocalWeatherFresh) ref.addTimestamp()
            return Limit(isReached = isLocalWeatherFresh)
        }
    }
    private suspend fun manageActivityRecommendationsLimits(
        currentTime: Timestamp,
        limitsDoc: DocumentReference
    ): Limit {
        return Limit(isReached = false)
    }

    private fun Date.formatNextUpdateDateTime(currentTime: Timestamp): String {
        val currentDate = currentTime.toDate()
        val currentDateCalendar = Calendar.getInstance()
        currentDateCalendar.time = currentDate

        val nextUpdateDateCalendar = Calendar.getInstance()
        nextUpdateDateCalendar.time = this

        var pattern = "HH:mm"
        if (currentDateCalendar.get(Calendar.DAY_OF_MONTH) != nextUpdateDateCalendar.get(Calendar.DAY_OF_MONTH)) pattern += ", dd MMM"
        if (currentDateCalendar.get(Calendar.YEAR) != nextUpdateDateCalendar.get(Calendar.YEAR)) pattern += ", yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(this)
    }

    private suspend fun Query.deleteDocs() {
        val batch = firestore.batch()
        this.get().await().documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit()
    }
    private fun CollectionReference.addTimestamp() {
        this.add(mapOf("timestamp" to FieldValue.serverTimestamp()))
    }

    private suspend fun getServerTimestamp(): Timestamp {
        val timestampRef = firestore.collection("serverTimestamp").document()
        timestampRef.set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
        val timestamp = timestampRef.get().await().getTimestamp("timestamp")
        timestampRef.delete()
        return timestamp!!
    }
}