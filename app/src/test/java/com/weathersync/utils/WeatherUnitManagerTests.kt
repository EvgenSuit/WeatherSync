package com.weathersync.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestException
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.userId
import com.weathersync.common.weather.mockWeatherUnitsManagerFirestore
import com.weathersync.features.settings.data.SelectedWeatherUnits
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.weather.Country
import com.weathersync.utils.weather.FirestoreWeatherUnit
import com.weathersync.utils.weather.WeatherUnitDocName
import com.weathersync.utils.weather.WeatherUnitsManager
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsWeatherUnitsTests {
    private val testException = TestException("exception")
    private lateinit var weatherUnitsManager: WeatherUnitsManager
    private lateinit var weatherUnitsManagerFirestore: FirebaseFirestore

    private fun setup(
        country: String = Country.US.name,
        unitDocNames: List<String>,
        units: List<FirestoreWeatherUnit>,
        unitsFetchException: Exception? = null,
        unitSetException: Exception? = null,) {
        weatherUnitsManagerFirestore = mockWeatherUnitsManagerFirestore(
            unitDocNames = unitDocNames,
            units = units,
            unitsFetchException = unitsFetchException,
            unitSetException = unitSetException,
        )
        weatherUnitsManager = WeatherUnitsManager(
            auth = mockAuth(),
            firestore = weatherUnitsManagerFirestore,
            country = country,
            )
    }

    @Test
    fun fetchUnits_allUnitsPresent_success() = runTest {
        val docNames = WeatherUnitDocName.entries.map { it.n }
        val units = listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.WindSpeed.MPH, WeatherUnit.Visibility.Meters)
        val firestoreUnits = units.map { FirestoreWeatherUnit(it.unitName) }
        setup(
            unitDocNames = docNames,
            units = firestoreUnits
        )
        val fetchedUnits = weatherUnitsManager.getUnits()
        assertEquals(SelectedWeatherUnits(
            temp = units[0] as WeatherUnit.Temperature,
            windSpeed = units[1] as WeatherUnit.WindSpeed,
            visibility = units[2] as WeatherUnit.Visibility
        ), fetchedUnits)
    }
    @Test(expected = TestException::class)
    fun fetchUnits_allUnitsPresent_exception() = runTest {
        val docNames = WeatherUnitDocName.entries.map { it.n }
        val units = listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.WindSpeed.MPH, WeatherUnit.Visibility.Meters)
        val firestoreUnits = units.map { FirestoreWeatherUnit(it.unitName) }
        setup(
            unitDocNames = docNames,
            units = firestoreUnits,
            unitsFetchException = testException
        )
        weatherUnitsManager.getUnits()
    }


    @Test
    fun fetchUnits_testDefaultTemperatureUnit_success() = runTest {
        val countries = listOf(Country.US, Country.GB)
        val expectedTemperatureUnits = listOf(WeatherUnit.Temperature.Fahrenheit, WeatherUnit.Temperature.Celsius)
        for ((c, tempUnit) in countries.zip(expectedTemperatureUnits)) {
            testDefaultTemperatureUnit(
                country = c,
                expectedTempUnit = tempUnit
            )
        }
    }
    @Test
    fun fetchUnits_testDefaultWindSpeedUnit_success() = runTest {
        val countries = listOf(Country.US, Country.JP)
        val expectedWindSpeedUnits = listOf(WeatherUnit.WindSpeed.MPH, WeatherUnit.WindSpeed.MS)
        for ((c, windSpeedUnit) in countries.zip(expectedWindSpeedUnits)) {
            testDefaultWindSpeedUnit(
                country = c,
                expectedWindSpeedUnit = windSpeedUnit
            )
        }
    }
    @Test
    fun fetchUnits_testDefaultVisibilityUnit_success() = runTest {
        val countries = listOf(Country.US, Country.JP)
        val expectedVisibilityUnits = listOf(WeatherUnit.Visibility.Miles, WeatherUnit.Visibility.Meters)
        for ((c, visibilityUnit) in countries.zip(expectedVisibilityUnits)) {
            testDefaultVisibilityUnit(
                country = c,
                expectedVisibilityUnit = visibilityUnit
            )
        }
    }

    @Test
    fun setUnit_success() = runTest {
        setup(
            unitDocNames = listOf(),
            units = listOf()
        )
        for (unit in listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.WindSpeed.MPH, WeatherUnit.Visibility.Meters)) {
            weatherUnitsManager.setUnit(unit)
            val docName = when (unit) {
                is WeatherUnit.Temperature -> WeatherUnitDocName.TEMP
                is WeatherUnit.WindSpeed -> WeatherUnitDocName.WIND_SPEED
                is WeatherUnit.Visibility -> WeatherUnitDocName.VISIBILITY
            }.n
            verify { weatherUnitsManagerFirestore
                .collection(userId).document("preferences").collection("weatherUnits")
                .document(docName).set(FirestoreWeatherUnit(unit.unitName)) }
        }
    }
    @Test(expected = TestException::class)
    fun setUnit_exception() = runTest {
        setup(
            unitDocNames = listOf(),
            units = listOf(),
            unitSetException = testException
        )
        for (unit in listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.WindSpeed.MPH, WeatherUnit.Visibility.Meters)) {
            weatherUnitsManager.setUnit(unit)
        }
    }

    private suspend fun testDefaultTemperatureUnit(
        country: Country,
        expectedTempUnit: WeatherUnit.Temperature
    ) {
        val docNames = WeatherUnitDocName.entries.filter { it != WeatherUnitDocName.TEMP }.map { it.n }
        val units = listOf(WeatherUnit.WindSpeed.MPH, WeatherUnit.Visibility.Meters)
        val firestoreUnits = units.map { FirestoreWeatherUnit(it.unitName) }
        setup(
            country = country.name,
            unitDocNames = docNames,
            units = firestoreUnits
        )
        val fetchedUnits = weatherUnitsManager.getUnits()
        assertEquals(SelectedWeatherUnits(
            temp = expectedTempUnit,
            windSpeed = units[0] as WeatherUnit.WindSpeed,
            visibility = units[1] as WeatherUnit.Visibility
        ), fetchedUnits)
    }
    private suspend fun testDefaultWindSpeedUnit(
        country: Country,
        expectedWindSpeedUnit: WeatherUnit.WindSpeed
    ) {
        val docNames = WeatherUnitDocName.entries.filter { it != WeatherUnitDocName.WIND_SPEED }.map { it.n }
        val units = listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.Visibility.Meters)
        val firestoreUnits = units.map { FirestoreWeatherUnit(it.unitName) }
        setup(
            country = country.name,
            unitDocNames = docNames,
            units = firestoreUnits
        )
        val fetchedUnits = weatherUnitsManager.getUnits()
        assertEquals(SelectedWeatherUnits(
            temp = units[0] as WeatherUnit.Temperature,
            windSpeed = expectedWindSpeedUnit,
            visibility = units[1] as WeatherUnit.Visibility
        ), fetchedUnits)
    }
    private suspend fun testDefaultVisibilityUnit(
        country: Country,
        expectedVisibilityUnit: WeatherUnit.Visibility
    ) {
        val docNames = WeatherUnitDocName.entries.filter { it != WeatherUnitDocName.VISIBILITY }.map { it.n }
        val units = listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.WindSpeed.MPH)
        val firestoreUnits = units.map { FirestoreWeatherUnit(it.unitName) }
        setup(
            country = country.name,
            unitDocNames = docNames,
            units = firestoreUnits
        )
        val fetchedUnits = weatherUnitsManager.getUnits()
        assertEquals(SelectedWeatherUnits(
            temp = units[0] as WeatherUnit.Temperature,
            windSpeed = units[1] as WeatherUnit.WindSpeed,
            visibility = expectedVisibilityUnit
        ), fetchedUnits)
    }
}