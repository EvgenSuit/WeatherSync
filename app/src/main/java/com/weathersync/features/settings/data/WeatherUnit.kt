package com.weathersync.features.settings.data

sealed class WeatherUnit(val unitName: String) {
    // weatherApiUnitName is used to make requests to OpenWeatherAPI (for visibility the same name is used)
    sealed class Temperature(unitName: String, val weatherApiUnitName: String): WeatherUnit(unitName) {
        data object Celsius: Temperature("°C", "celsius")
        data object Fahrenheit: Temperature("°F", "fahrenheit")
    }
    sealed class Visibility(unitName: String): WeatherUnit(unitName) {
        data object Meters: Visibility("m")
        data object Kilometers: Visibility("km")
        data object Miles: Visibility("mi")
    }
    sealed class WindSpeed(unitName: String, val weatherApiUnitName: String): WeatherUnit(unitName) {
        data object KMH: WindSpeed("km/h", "kmh")
        data object MS: WindSpeed("m/s", "ms")
        data object MPH: WindSpeed("mi/h", "mph")
    }
}
data class SelectedWeatherUnits(
    val temp: WeatherUnit.Temperature,
    val windSpeed: WeatherUnit.WindSpeed,
    val visibility: WeatherUnit.Visibility
)