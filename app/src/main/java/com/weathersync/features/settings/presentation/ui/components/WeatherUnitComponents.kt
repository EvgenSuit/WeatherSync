package com.weathersync.features.settings.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.R
import com.weathersync.features.settings.data.SelectedWeatherUnits
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.ui.theme.WeatherSyncTheme

@Composable
fun WeatherUnitsComponent(
    enabled: Boolean,
    selectedWeatherUnits: SelectedWeatherUnits?,
    onWeatherUnitSelected: (WeatherUnit) -> Unit
) {
    var isTempDropdownExpanded by rememberSaveable {
        mutableStateOf(false)
    }
    var isWindSpeedDropdownExpanded by rememberSaveable {
        mutableStateOf(false)
    }
    var isVisibilityDropdownExpanded by rememberSaveable {
        mutableStateOf(false)
    }
    CommonSettingsComponent(textId = R.string.temp_unit) {
        WeatherUnitDropdown(
            enabled = enabled,
            selectedUnit = selectedWeatherUnits?.temp,
            allUnits = listOf(WeatherUnit.Temperature.Celsius, WeatherUnit.Temperature.Fahrenheit),
            expanded = isTempDropdownExpanded,
            onExpandedChange = { isTempDropdownExpanded = it },
            onItemSelected = { selected ->
                onWeatherUnitSelected(selected)
            }
        )
    }

    // Wind speed dropdown
    CommonSettingsComponent(textId = R.string.wind_speed_unit) {
        WeatherUnitDropdown(
            enabled = enabled,
            selectedUnit = selectedWeatherUnits?.windSpeed,
            allUnits = listOf(WeatherUnit.WindSpeed.KMH, WeatherUnit.WindSpeed.MS, WeatherUnit.WindSpeed.MPH),
            expanded = isWindSpeedDropdownExpanded,
            onExpandedChange = { isWindSpeedDropdownExpanded = it },
            onItemSelected = { selected ->
                onWeatherUnitSelected(selected)
            }
        )
    }

    // Visibility dropdown
    CommonSettingsComponent(textId = R.string.visibility_unit) {
        WeatherUnitDropdown(
            enabled = enabled,
            selectedUnit = selectedWeatherUnits?.visibility,
            allUnits = listOf(WeatherUnit.Visibility.Meters, WeatherUnit.Visibility.Kilometers, WeatherUnit.Visibility.Miles),
            expanded = isVisibilityDropdownExpanded,
            onExpandedChange = { isVisibilityDropdownExpanded = it },
            onItemSelected = { selected ->
                onWeatherUnitSelected(selected)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T: WeatherUnit> RowScope.WeatherUnitDropdown(
    enabled: Boolean,
    selectedUnit: T?,
    allUnits: List<T>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemSelected: (T) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.weight(0.5f)
    ) {
        TextField(
            value = selectedUnit?.unitName ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = selectedUnit != null && enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            allUnits.forEach { unit ->
                DropdownMenuItem(
                    enabled = enabled,
                    text = { Text(unit.unitName) },
                    onClick = {
                        onItemSelected(unit)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.testTag("Dropdown")
                )
            }
        }
    }
}

@Preview
@Composable
fun WeatherUnitsPreview() {
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                WeatherUnitsComponent(
                    enabled = false,
                    selectedWeatherUnits = SelectedWeatherUnits(
                        temp = WeatherUnit.Temperature.Celsius,
                        windSpeed = WeatherUnit.WindSpeed.KMH,
                        visibility = WeatherUnit.Visibility.Meters
                    ),
                    onWeatherUnitSelected = {}
                )
            }
        }
    }
}