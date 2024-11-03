package com.weathersync.features.activityPlanning.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathersync.R
import com.weathersync.common.ui.CustomButton
import com.weathersync.common.ui.CustomCircularProgressIndicator
import com.weathersync.common.ui.TextFieldState
import com.weathersync.common.ui.UIText
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningIntent
import com.weathersync.features.activityPlanning.presentation.maxActivityInputLength
import com.weathersync.features.home.presentation.ui.components.CommonHomeComponent
import com.weathersync.ui.theme.WeatherSyncTheme

@Composable
fun PlanYourActivityComposable(
    textFieldState: TextFieldState,
    isInProgress: Boolean,
    output: String?,
    onIntent: (ActivityPlanningIntent) -> Unit
) {
    val horizontalDividerPadding = dimensionResource(id = R.dimen.horizontal_divider_padding)
    val context = LocalContext.current
    val text = textFieldState.value
    val inputError = textFieldState.error
    val isErrorNotEmpty = inputError != null && inputError !is UIText.Empty
    CommonHomeComponent(titleRes = R.string.plan_your_activities) {
        if (!isInProgress) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { onIntent(ActivityPlanningIntent.Input(it)) },
                    isError = isErrorNotEmpty,
                    maxLines = 13,
                    supportingText = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // error will not be displayed if no input was made
                            if (isErrorNotEmpty) {
                                Text(text = inputError!!.asString(context),
                                    modifier = Modifier.weight(0.5f))
                            }
                            Text(text = "${text.length} / $maxActivityInputLength",
                                modifier = Modifier.weight(0.5f),
                                textAlign = TextAlign.End)
                        }
                    },
                    placeholder = {
                        Text(text = stringResource(id = R.string.activity_text_field_placeholder),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ActivityTextField"),
                )
            }
            CustomButton(enabled = inputError is UIText.Empty,
                text = stringResource(id = R.string.find_optimal_times),
                onClick = { onIntent(ActivityPlanningIntent.GenerateRecommendations) })
            if (!output.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(horizontalDividerPadding))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8f))
                }
                Spacer(modifier = Modifier.height(horizontalDividerPadding))
                Text(text = output,
                    style = TextStyle(
                        fontSize = 15.sp
                    ))
            }
            Spacer(modifier = Modifier.height(horizontalDividerPadding))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val infoIcon = Icons.Filled.Info
                Icon(imageVector = infoIcon,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(15.dp),
                    contentDescription = infoIcon.name)
                Text(text = stringResource(id = R.string.based_on_forecast, 5),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                    ))
            }
        } else {
            Text(text = stringResource(id = R.string.planning_activities))
            Spacer(modifier = Modifier.height(12.dp))
            CustomCircularProgressIndicator(modifier = Modifier.testTag("ActivityPlannerProgress"))
        }
    }
}

@Preview
@Composable
fun PlanYourActivityComposablePreview() {
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            PlanYourActivityComposable(
                textFieldState = TextFieldState(
                    //error = UIText.DynamicString("Text cannot be empty. ".repeat(4))
                ),
                isInProgress = false,
                output = "Recommended times".repeat(30),
                onIntent = {}
            )
        }
    }
}