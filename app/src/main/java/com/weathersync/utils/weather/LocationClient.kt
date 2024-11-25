package com.weathersync.utils.weather

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.weathersync.features.home.data.Coordinates
import com.weathersync.utils.LocationRequestException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
class LocationClient(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val geocoder: Geocoder
) {
    @SuppressLint("MissingPermission")
    suspend fun getCoordinates(): Coordinates = suspendCancellableCoroutine { continuation ->
        val task = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener { location ->
            if (location != null) {
                processLocation(location, continuation)
            } else {
                try {
                    requestUpToDateLocation(continuation)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }.addOnFailureListener { e ->
            continuation.resumeWithException(e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestUpToDateLocation(continuation: CancellableContinuation<Coordinates>) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000)
            .setMaxUpdates(1)
            .build()
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationProviderClient.removeLocationUpdates(this)
                val newLocation = locationResult.lastLocation
                if (newLocation != null) {
                    processLocation(newLocation, continuation)
                } else {
                    continuation.resumeWithException(LocationRequestException("Unable to fetch up-to-date location: location is null. Location result: $locationResult"))
                }
            }
        }, Looper.getMainLooper())
    }

    private fun processLocation(location: Location, continuation: CancellableContinuation<Coordinates>) {
        try {
            val locality = getLocality(location)
            continuation.resume(Coordinates(
                locality = locality,
                lat = location.latitude,
                lon = location.longitude), null)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    private fun getLocality(location: Location): String {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (addresses.isNullOrEmpty()) return "null"
        val address = addresses[0]!!
        // locality - city name, subLocality - district/neighborhood, adminArea - state/province
        val additionalInfo = address.locality ?: address.subLocality ?: address.adminArea
        return "${if (additionalInfo != null) "${additionalInfo}, " else ""}${address.countryName}"
    }
}