package com.weathersync.utils.location

import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.model.LatLng
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.auth.userId
import com.weathersync.common.utils.location.mockLocationManager
import com.weathersync.common.weather.LocationInfo
import com.weathersync.common.weather.fullLocation
import com.weathersync.common.weather.mockAndroidLocationClient
import com.weathersync.utils.NoGoogleMapsGeocodingResult
import com.weathersync.utils.weather.location.AndroidLocationClient
import com.weathersync.utils.weather.location.LocationManager
import com.weathersync.utils.weather.location.data.LocationPreference
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocationManagerTests {
    @get: Rule
    val dispatcherRule = MainDispatcherRule()
    private lateinit var locationManager: LocationManager
    private val firestore = mockk<FirebaseFirestore>()
    private val preferenceSetSlot = slot<LocationPreference>()
    private lateinit var androidLocationClient: AndroidLocationClient
    private val inputLocation = "Warsaw, Poland"
    private val currLocationInfo = LocationInfo()
    private val inputLatLong = LatLng(51.107883, 17.038538)

    private fun setup(isSavedLocationNull: Boolean = false,
                      areLocationManagerResultsEmpty: Boolean = false,
                      latLng: LatLng = inputLatLong) {
        androidLocationClient = mockAndroidLocationClient(locationInfo = currLocationInfo)
        locationManager = mockLocationManager(
            inputLocation = inputLocation,
            isSavedLocationNull = isSavedLocationNull,
            areLocationManagerResultsEmpty = areLocationManagerResultsEmpty,
            latLng = latLng,
            firestore = firestore,
            preferenceSetSlot = preferenceSetSlot,
            androidLocationClient = androidLocationClient,
            dispatcher = dispatcherRule.testDispatcher
        )
    }

    @Before
    fun init() {
        setup()
    }

    @Test
    fun setLocation_success() = runTest {
        assertEquals(inputLocation, locationManager.setLocation(inputLocation))
        verify(exactly = 1) {
            firestore.collection(userId).document("preferences").collection("location")
                .document("currLocation").set(any())
        }
        assertEquals(inputLocation, preferenceSetSlot.captured.location)
    }
    @Test
    fun setLocation_noGeocodingResult() = runTest {
        setup(areLocationManagerResultsEmpty = true)
        assertFailsWith<NoGoogleMapsGeocodingResult> { locationManager.setLocation(inputLocation) }
        verify(inverse = true) {
            firestore.collection(userId).document("preferences").collection("location")
                .document("currLocation").set(any())
        }
    }

    @Test
    fun setCurrLocationAsDefault_success() = runTest {
        val fullLocation = currLocationInfo.fullLocation()
        assertEquals(fullLocation, locationManager.setCurrLocationAsDefault())

        verify(exactly = 1) {
            firestore.collection(userId).document("preferences").collection("location")
                .document("currLocation").set(any())
        }

        val capturedPreference = preferenceSetSlot.captured
        assertEquals(currLocationInfo.latitude, capturedPreference.lat)
        assertEquals(currLocationInfo.longitude, capturedPreference.lon)
        assertEquals(fullLocation, capturedPreference.location)
    }

    @Test
    fun getLocation_savedLocationNull_returnedLocationIsCurrent() = runTest {
        setup(isSavedLocationNull = true)
        val fullLocation = currLocationInfo.fullLocation()

        val preference = locationManager.getLocation()
        verify(exactly = 1) {
            firestore.collection(userId).document("preferences").collection("location")
                .document("currLocation").get()
        }
        coVerify(exactly = 1) { androidLocationClient.getCoordinates() }
        assertEquals(currLocationInfo.latitude, preference.lat)
        assertEquals(currLocationInfo.longitude, preference.lon)
        assertEquals(fullLocation, preference.location)
    }
    @Test
    fun getLocation_savedLocationNotNull_returnedLocationIsSavedOne() = runTest {
        setup(isSavedLocationNull = false)

        val preference = locationManager.getLocation()
        verify(exactly = 1) {
            firestore.collection(userId).document("preferences").collection("location")
                .document("currLocation").get()
        }
        coVerify(inverse = true) { androidLocationClient.getCoordinates() }
        assertEquals(inputLatLong.lat, preference.lat)
        assertEquals(inputLatLong.lng, preference.lon)
        assertEquals(inputLocation, preference.location)
    }
}