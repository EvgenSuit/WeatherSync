package com.weathersync.features.home

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.weathersync.features.home.data.Coordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.TimeZone
import kotlin.coroutines.resumeWithException

class LocationClient(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val geocoder: Geocoder
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    suspend fun getCoordinates(): Coordinates = suspendCancellableCoroutine { continuation ->
        val task = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener { location ->
            if (location != null) {
                try {
                    //scope.launch(Dispatchers.IO) {
                        val locality = getLocality(location)
                        continuation.resume(Coordinates(
                            locality = locality,
                            lat = location.latitude,
                            lon = location.longitude), null)
                   // }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            } else {
                continuation.resumeWithException(Exception("Location is null"))
            }
        }.addOnFailureListener { e ->
            continuation.resumeWithException(e)
        }
    }
    private fun getLocality(location: Location): String {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val address = addresses?.get(0)!!
        return "${address.locality}, ${address.countryName}"
    }
}