package com.weathersync.common.weather

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.userId
import com.weathersync.common.mockTask
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.weather.Country
import com.weathersync.utils.weather.FirestoreWeatherUnit
import com.weathersync.utils.weather.WeatherUnitDocName
import com.weathersync.utils.weather.WeatherUnitsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk

fun mockWeatherUnitsManager(
    firestoreUnits: List<FirestoreWeatherUnit> = fetchedFirestoreWeatherUnits,
    unitsFetchException: Exception? = null,
    unitSetException: Exception? = null
): WeatherUnitsManager = spyk(
        WeatherUnitsManager(
        auth = mockAuth(),
        firestore = mockWeatherUnitsManagerFirestore(
            unitDocNames = WeatherUnitDocName.entries.map { it.n },
            units = firestoreUnits,
            unitSetException = unitSetException,
            unitsFetchException = unitsFetchException
        ),
        country = Country.US.name
    )
    )

fun mockWeatherUnitsManagerFirestore(
    unitDocNames: List<String>,
    units: List<FirestoreWeatherUnit>,
    unitsFetchException: Exception? = null,
    unitSetException: Exception? = null,
): FirebaseFirestore = mockk {
            every {
                collection(userId).document("preferences").collection("weatherUnits").get()
            } returns mockTask(
                data = mockk {
                    every { documents } returns units.mapIndexed { i, firestoreWeatherUnit ->
                        mockk {
                            every { id } returns unitDocNames[i]
                            every { toObject<FirestoreWeatherUnit>() } returns firestoreWeatherUnit
                        }
                    }
                },
                taskException = unitsFetchException
            )
            WeatherUnitDocName.entries.forEach { docName ->
                every { collection(userId).document("preferences").collection("weatherUnits")
                    .document(docName.n).set(any<FirestoreWeatherUnit>()) } returns mockTask(taskException = unitSetException)
            }
        }
val fetchedWeatherUnits = listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.WindSpeed.MPH, WeatherUnit.Visibility.Meters)
val fetchedFirestoreWeatherUnits = fetchedWeatherUnits.map { FirestoreWeatherUnit(it.unitName) }