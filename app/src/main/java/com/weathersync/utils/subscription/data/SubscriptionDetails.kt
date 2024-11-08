package com.weathersync.utils.subscription.data

data class SubscriptionDetails(
    val productId: String,
    val title: String,
    val description: String,
    val offers: List<OfferDetails>
)

data class OfferDetails(
    val offerId: String?,
    val pricingPhases: List<PricingPhaseDetails>
)

data class PricingPhaseDetails(
    val priceCurrencyCode: String,
    val priceAmount: Double,
    val billingPeriod: String,
    val recurrenceMode: Int,
    val isFreeTrial: Boolean = false
)

