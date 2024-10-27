package com.weathersync.common.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.weathersync.common.auth.userId
import com.weathersync.common.mockTask
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.FirestoreWeatherUnit
import com.weathersync.utils.WeatherUnitDocName
import io.mockk.every
import io.mockk.mockk

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