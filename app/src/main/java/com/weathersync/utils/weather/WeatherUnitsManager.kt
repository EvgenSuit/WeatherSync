package com.weathersync.utils.weather

import androidx.annotation.Keep
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.weathersync.features.settings.data.SelectedWeatherUnits
import com.weathersync.features.settings.data.WeatherUnit
import kotlinx.coroutines.tasks.await

enum class WeatherUnitDocName(val n: String) {
    TEMP("temp"),
    WIND_SPEED("windSpeed"),
    VISIBILITY("visibility")
}

enum class Country {
    US, BS, LR, MM, GB, JP, NO
}

class WeatherUnitsManager(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    private val country: String
) {
    private val unitsRef = firestore.collection(auth.currentUser!!.uid)
        .document("preferences").collection("weatherUnits")

    private fun getDefaultTemperatureUnit(): WeatherUnit.Temperature {
        return when (country) {
            Country.US.name, Country.BS.name, Country.LR.name, Country.MM.name -> WeatherUnit.Temperature.Fahrenheit
            else -> WeatherUnit.Temperature.Celsius
        }
    }

    private fun getDefaultWindSpeedUnit(): WeatherUnit.WindSpeed {
        return when (country) {
            Country.US.name, Country.GB.name -> WeatherUnit.WindSpeed.MPH
            Country.JP.name, Country.NO.name -> WeatherUnit.WindSpeed.MS
            else -> WeatherUnit.WindSpeed.KMH
        }
    }

    private fun getDefaultVisibilityUnit(): WeatherUnit.Visibility {
        return when (country) {
            Country.US.name, Country.GB.name-> WeatherUnit.Visibility.Miles
            Country.JP.name -> WeatherUnit.Visibility.Meters
            else -> WeatherUnit.Visibility.Kilometers
        }
    }

    // Map strings to WeatherUnit objects
    private fun getTemperatureUnit(unitName: String?): WeatherUnit.Temperature {
        return when (unitName) {
            WeatherUnit.Temperature.Celsius.unitName -> WeatherUnit.Temperature.Celsius
            WeatherUnit.Temperature.Fahrenheit.unitName -> WeatherUnit.Temperature.Fahrenheit
            else -> getDefaultTemperatureUnit()
        }
    }

    private fun getWindSpeedUnit(unitName: String?): WeatherUnit.WindSpeed {
        return when (unitName) {
            WeatherUnit.WindSpeed.KMH.unitName -> WeatherUnit.WindSpeed.KMH
            WeatherUnit.WindSpeed.MS.unitName -> WeatherUnit.WindSpeed.MS
            WeatherUnit.WindSpeed.MPH.unitName -> WeatherUnit.WindSpeed.MPH
            else -> getDefaultWindSpeedUnit()
        }
    }

    private fun getVisibilityUnit(unitName: String?): WeatherUnit.Visibility {
        return when (unitName) {
            WeatherUnit.Visibility.Meters.unitName -> WeatherUnit.Visibility.Meters
            WeatherUnit.Visibility.Kilometers.unitName -> WeatherUnit.Visibility.Kilometers
            WeatherUnit.Visibility.Miles.unitName -> WeatherUnit.Visibility.Miles
            else -> getDefaultVisibilityUnit()
        }
    }

    suspend fun setUnit(unit: WeatherUnit) {
        val docName = when (unit) {
            is WeatherUnit.Temperature -> WeatherUnitDocName.TEMP
            is WeatherUnit.WindSpeed -> WeatherUnitDocName.WIND_SPEED
            is WeatherUnit.Visibility -> WeatherUnitDocName.VISIBILITY
        }.n
        unitsRef.document(docName).set(FirestoreWeatherUnit(unit.unitName)).await()
    }

    suspend fun getUnits(): SelectedWeatherUnits {
        val units = unitsRef.get().await()
        return SelectedWeatherUnits(
            temp = getTemperatureUnit(units.documents.firstOrNull { it.id == WeatherUnitDocName.TEMP.n }
                .getUnitName()),
            windSpeed = getWindSpeedUnit(units.documents.firstOrNull { it.id == WeatherUnitDocName.WIND_SPEED.n }
                .getUnitName()),
            visibility = getVisibilityUnit(units.documents.firstOrNull { it.id == WeatherUnitDocName.VISIBILITY.n }
                .getUnitName())
        )
    }
    private fun DocumentSnapshot?.getUnitName() = this?.toObject<FirestoreWeatherUnit>()?.unitName

}
@Keep
data class FirestoreWeatherUnit(val unitName: String = "")