package com.weathersync.features.subscription.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.weathersync.MainActivity
import com.weathersync.R
import com.weathersync.common.ui.ConstrainedComponent
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.PrivacyTermsLinks
import com.weathersync.features.subscription.presentation.SubscriptionInfoUIState
import com.weathersync.features.subscription.presentation.SubscriptionInfoViewModel
import com.weathersync.features.subscription.presentation.ui.components.BenefitsComponent
import com.weathersync.features.subscription.presentation.ui.components.LoadingSubscription
import com.weathersync.features.subscription.presentation.ui.components.SubscriptionOptions
import com.weathersync.ui.SubscriptionUIEvent
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.isInProgress
import com.weathersync.utils.subscription.data.OfferDetails
import com.weathersync.utils.subscription.data.PricingPhaseDetails
import com.weathersync.utils.subscription.data.SubscriptionDetails
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionInfoScreen(viewModel: SubscriptionInfoViewModel = koinViewModel(),
                           activity: MainActivity,
                           onBackClick: () -> Unit) {
    val snackbarController = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.handleIntent(SubscriptionScreenIntent.FetchSubscriptionDetails)
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is SubscriptionUIEvent.ShowSnackbar -> snackbarController.showSnackbar(event.message)
                is SubscriptionUIEvent.NavigateUp -> onBackClick()
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.purchasesUpdatedEvent.collectLatest { event ->
            when (event) {
                is SubscriptionUIEvent.ShowSnackbar -> snackbarController.showSnackbar(event.message)
                else -> {}
            }
        }
    }
    SubscriptionInfoContent(
        activity = activity,
        uiState = uiState,
        onBackClick = onBackClick,
        onIntent = viewModel::handleIntent)
}

@Composable
fun SubscriptionInfoContent(
    activity: MainActivity,
    uiState: SubscriptionInfoUIState,
    onBackClick: () -> Unit,
    onIntent: (SubscriptionScreenIntent) -> Unit
) {
    val subscriptionDetails = uiState.subscriptionDetails
    val isFetchInProgress = uiState.infoFetchResult.isInProgress()
    val backgroundGradient = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.primary))
    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundGradient)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBackClick) {
                    val icon = Icons.AutoMirrored.Default.ArrowBack
                    Icon(imageVector = icon, contentDescription = icon.name)
                }
            }
            Text(
                text = stringResource(id = R.string.go_premium),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(id = R.string.cancel_anytime),
                style = MaterialTheme.typography.labelMedium
                    .copy(fontSize = 19.sp)
            )
            Spacer(modifier = Modifier.height(100.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ConstrainedComponent(modifier = Modifier.padding(20.dp)) {
                    if (subscriptionDetails == null || isFetchInProgress) {
                        LoadingSubscription()
                    } else {
                        BenefitsComponent()
                        SubscriptionOptions(subscriptionDetails = subscriptionDetails)
                        ElevatedButton(
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color.Yellow,
                                contentColor = Color.Black
                            ),
                            onClick = { onIntent(SubscriptionScreenIntent.Purchase(activity)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Text(
                                stringResource(id = R.string.start_plan),
                                style = MaterialTheme.typography.labelSmall
                                    .copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                    PrivacyTermsLinks(modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}

@Preview(device = "spec:id=reference_phone,shape=Normal,width=511,height=891,unit=dp,dpi=420")
@Composable
fun NullSubscriptionInfoContentPreview() {
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            SubscriptionInfoContent(
                activity = MainActivity(),
                uiState = SubscriptionInfoUIState(
                    subscriptionDetails = null
                ),
                onBackClick = {},
                onIntent = {}
            )
        }
    }
}
@Preview
@Composable
fun NotNullSubscriptionInfoContentPreview() {
    val subscriptionDetails = listOf(
        SubscriptionDetails(
            productId = "",
            title = "Premium Plan",
            description = "",
            offers = listOf(
                OfferDetails(
                    offerId = "premium_offer",
                    pricingPhases = listOf(
                        PricingPhaseDetails(
                            priceCurrencyCode = "PLN",
                            priceAmount = 19.99,
                            billingPeriod = "P2D",
                            recurrenceMode = 1, // Example recurrence mode
                            isFreeTrial = true
                        ),
                        PricingPhaseDetails(
                            priceCurrencyCode = "PLN",
                            priceAmount = 19.99, // 19.99 PLN
                            billingPeriod = "P1M", // 1 month
                            recurrenceMode = 1, // Example recurrence mode
                            isFreeTrial = false
                        )
                    )
                )
            )
        )
    )
    WeatherSyncTheme(darkTheme = true) {
        Surface {
            SubscriptionInfoContent(
                activity = MainActivity(),
                uiState = SubscriptionInfoUIState(
                    subscriptionDetails = subscriptionDetails
                ),
                onBackClick = {},
                onIntent = {}
            )
        }
    }
}