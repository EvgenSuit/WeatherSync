package com.weathersync.utils.weather.location

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.weathersync.BuildConfig
import com.weathersync.utils.NoGoogleMapsGeocodingResult
import com.weathersync.utils.NullFirebaseUser
import com.weathersync.utils.weather.location.data.LocationPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocationManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val geoApiContext: GeoApiContext,
    private val androidLocationClient: AndroidLocationClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private fun getGeocodeResult(inputLocation: String): LocationPreference {
        val results = GeocodingApi.geocode(geoApiContext, inputLocation).await()
        if (results.isNotEmpty()) {
            val result = results[0]
            val geometry = result.geometry.location
            return LocationPreference(
                location = result.formattedAddress,
                lat = geometry.lat,
                lon = geometry.lng
            )
        } else throw NoGoogleMapsGeocodingResult("Input: $inputLocation")
    }

    suspend fun setLocation(inputLocation: String): String = withContext(dispatcher) {
        val info = getGeocodeResult(inputLocation)
        val currUser = auth.currentUser ?: throw NullFirebaseUser()
        firestore.collection(currUser.uid).document("preferences").collection("location")
            .document("currLocation").set(info).await()
        info.location
    }
    suspend fun setCurrLocationAsDefault(): String = withContext(dispatcher) {
        val currUserId = auth.currentUser?.uid ?: throw NullFirebaseUser()
        val currLocation = androidLocationClient.getCoordinates()
        firestore.collection(currUserId).document("preferences")
            .collection("location").document("currLocation").set(
                LocationPreference(
                    lat = currLocation.lat,
                    lon = currLocation.lon,
                    location = currLocation.locality
                )
            ).await()
        currLocation.locality
    }
    // this method is already called from IO dispatcher context from home and activity planning repositories
    suspend fun getLocation(): LocationPreference {
        val currUserId = auth.currentUser?.uid ?: throw NullFirebaseUser()
        val savedLocation = firestore.collection(currUserId).document("preferences")
            .collection("location").document("currLocation").get().await().toObject<LocationPreference>()
        if (savedLocation == null) {
            val currLocation = androidLocationClient.getCoordinates()
            return LocationPreference(
                lat = currLocation.lat,
                lon = currLocation.lon,
                location = currLocation.locality
            )
        } else return LocationPreference(
            lat = savedLocation.lat,
            lon = savedLocation.lon,
            location = savedLocation.location
        )
    }
}