package com.weathersync.features.home.presentation.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weathersync.R
import com.weathersync.common.ui.CustomCircularProgressIndicator
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.ui.theme.WeatherSyncTheme

@Composable
fun CurrentWeatherComposable(weather: CurrentWeather?,
                             isFetchInProgress: Boolean) {
    val labelStyle = MaterialTheme.typography.labelMedium
    CommonHomeComponent(titleRes = R.string.current_weather) {
        if (!isFetchInProgress && weather != null) {
            Text(text = "${weather.temp} ${weather.tempUnit}",
                style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = weather.locality,
                    style = labelStyle,
                    modifier = Modifier.weight(1.2f))
                Spacer(modifier = Modifier.weight(0.8f))
                Text(text = stringResource(id = R.string.wind_speed, "${weather.windSpeed} ${weather.windSpeedUnit}"),
                    style = labelStyle,
                    modifier = Modifier.weight(0.8f))
            }
        } else CustomCircularProgressIndicator(modifier = Modifier.testTag("CurrentWeatherProgress"))
        }
}


@Preview
@Composable
fun CurrentWeatherComposablePreview() {
    WeatherSyncTheme {
        Surface {
            CurrentWeatherComposable(weather = CurrentWeather(
                locality = "Mountain View, United States of America",
                tempUnit = "°C",
                windSpeedUnit = "km/h",
                temp = 16.4,
                windSpeed = 10.3,
                weatherCode = 0
            ),
                isFetchInProgress = true)
        }
    }
}