package com.weathersync.features.subscription.presentation.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathersync.R
import com.weathersync.utils.subscription.data.OfferDetails
import com.weathersync.utils.subscription.data.PricingPhaseDetails
import com.weathersync.utils.subscription.data.SubscriptionDetails
import java.time.Period
import java.util.Locale


@Composable
fun SubscriptionOptions(
    subscriptionDetails: List<SubscriptionDetails>,
) {
    // Find the offer with a free trial phase if available
    val freeTrialOffer = subscriptionDetails
        .flatMap { it.offers }
        .firstOrNull { offer ->
            offer.pricingPhases.any { phase -> phase.isFreeTrial }
        }

    // Find the first basic paid plan if no free trial offer is available
    val basicPaidPlan = subscriptionDetails.firstOrNull()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (freeTrialOffer != null) {
            FreeTrialOption(freeTrialOffer)
        } else if (basicPaidPlan != null) {
            BasicPaidPlanOption(basicPaidPlan)
        }
    }
}

@Composable
fun FreeTrialOption(offer: OfferDetails) {
    val freeTrialPhase = offer.pricingPhases.first { it.isFreeTrial }
    val afterTrialPhase = offer.pricingPhases[1]
    val freeTrialBillingPeriodText = formatBillingPeriod(LocalContext.current, freeTrialPhase.billingPeriod, includeNumber = true)
    val afterTrialBillingPeriodText = formatBillingPeriod(LocalContext.current, afterTrialPhase.billingPeriod, includeNumber = false)
    PlanCard(
        title = stringResource(id = R.string.free_trial, freeTrialBillingPeriodText),
        bodyText = stringResource(id = R.string.after_free_trial, formatPrice(afterTrialPhase, afterTrialBillingPeriodText))
    )
}

@Composable
fun BasicPaidPlanOption(subscription: SubscriptionDetails) {
    val firstPaidPhase = subscription.offers.flatMap { it.pricingPhases }.firstOrNull { !it.isFreeTrial }
    if (firstPaidPhase != null) {
        val billingPeriodText = formatBillingPeriod(LocalContext.current, firstPaidPhase.billingPeriod, includeNumber = false)
        PlanCard(
            title = subscription.title,
            bodyText = formatPrice(firstPaidPhase, billingPeriodText)
        )
    }
}

@Composable
fun PlanCard(
    title: String,
    bodyText: String) {
    val shape = RoundedCornerShape(20.dp)
    ElevatedCard(
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
            .shadow(
                spotColor = MaterialTheme.colorScheme.primary,
                elevation = 10.dp,
                shape = shape
            ),
    ) {
        Column(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.displayMedium)
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8f))
            Text(text = bodyText,
                fontSize = 12.sp)

        }
    }
}

fun formatBillingPeriod(context: Context, billingPeriod: String, includeNumber: Boolean): String {
    val period = Period.parse(billingPeriod)
    val resources = context.resources
    val months = period.months.let { if (it != 0) {
        if (includeNumber) "$it-${resources.getString(R.string.month)}" else {
             resources.getString(R.string.month)
        }
    } else null }
    val days = period.days.let { if (it != 0) {
        if (includeNumber) "$it-${resources.getString(R.string.day)}" else {
            resources.getString(R.string.day)
        }
    } else null }

    // Join all non-null results into a string, separated by spaces
    return listOfNotNull(months, days).joinToString(" ")
}



fun formatPrice(pricingPhase: PricingPhaseDetails, billingDuration: String): String {
    return "${pricingPhase.priceAmount} ${pricingPhase.priceCurrencyCode}/$billingDuration"
}
