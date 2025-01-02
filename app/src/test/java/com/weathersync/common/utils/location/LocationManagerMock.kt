package com.weathersync.common.utils.location

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.GeocodingApi
import com.google.maps.model.GeocodingResult
import com.google.maps.model.Geometry
import com.google.maps.model.LatLng
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.userId
import com.weathersync.common.mockTask
import com.weathersync.common.weather.LocationInfo
import com.weathersync.common.weather.fullLocation
import com.weathersync.utils.weather.location.AndroidLocationClient
import com.weathersync.utils.weather.location.LocationManager
import com.weathersync.utils.weather.location.data.LocationPreference
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.test.TestDispatcher

fun mockLocationManager(
    inputLocation: String = LocationInfo().fullLocation(),
    latLng: LatLng = LatLng(51.107883, 17.038538),
    firestore: FirebaseFirestore = mockk(),
    areLocationManagerResultsEmpty: Boolean = false,
    isSavedLocationNull: Boolean = true,
    preferenceSetSlot: CapturingSlot<LocationPreference> = slot(),
    locationManagerException: Exception? = null,
    androidLocationClient: AndroidLocationClient,
    dispatcher: TestDispatcher): LocationManager {
    mockkStatic(GeocodingApi::class)
    val mockedResult = GeocodingResult().apply {
        formattedAddress = inputLocation
        geometry = Geometry().apply {
            location = latLng
        }
    }
    val location = mockedResult.geometry.location
    every { GeocodingApi.geocode(any(), any()).await() } answers {
        if (locationManagerException != null) throw locationManagerException
        if (areLocationManagerResultsEmpty) arrayOf() else arrayOf(mockedResult)
    }
    every { firestore.collection(userId).document("preferences").collection("location")
        .document("currLocation").get() } returns mockTask(
            data = mockk {
                every { toObject(LocationPreference::class.java) } returns if (isSavedLocationNull) null
                else LocationPreference(
                    location = inputLocation,
                    lat = location.lat,
                    lon = location.lng
                )
            }
        )
    every { firestore.collection(userId).document("preferences").collection("location")
        .document("currLocation").set(capture(preferenceSetSlot)) } returns mockTask()
    return spyk(LocationManager(
        auth = mockAuth(),
        firestore = firestore,
        geoApiContext = mockk(relaxed = true),
        androidLocationClient = androidLocationClient,
        dispatcher = dispatcher
    ))
}