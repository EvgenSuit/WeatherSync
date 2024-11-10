package com.weathersync.utils.subscription

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.weathersync.R
import com.weathersync.common.ui.UIText
import com.weathersync.ui.SubscriptionUIEvent
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.BillingServiceDisconnected
import com.weathersync.utils.BillingServiceInitException
import com.weathersync.utils.PurchasesUpdatedException
import com.weathersync.utils.SubscriptionCheckException
import com.weathersync.utils.subscription.data.OfferDetails
import com.weathersync.utils.subscription.data.PricingPhaseDetails
import com.weathersync.utils.subscription.data.SubscriptionDetails
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

typealias IsBillingSetupFinished = Boolean
typealias IsSubscribed = Boolean

class SubscriptionManager(
    billingClientBuilder: BillingClient.Builder,
    private val subscriptionInfoDatastore: SubscriptionInfoDatastore,
    private val analyticsManager: AnalyticsManager
) {
    private val billingClientInitMutex = Mutex()
    private val isSubscribedMutex = Mutex()

    private val _purchasesUpdatedEvent = MutableSharedFlow<SubscriptionUIEvent>()
    val purchasesUpdatedEvent = _purchasesUpdatedEvent.asSharedFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        CoroutineScope(Dispatchers.IO).launch {
            if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    try {
                        handlePurchase(purchase)
                        val isSubscribed = fetchUpToDateSubscriptionState()
                        subscriptionInfoDatastore.setIsSubscribed(isSubscribed)
                    } catch (e: Exception) {
                        _purchasesUpdatedEvent.emit(SubscriptionUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_update_purchases)))
                        analyticsManager.recordException(e)
                    }
                }
            } else if (billingResult.responseCode != BillingResponseCode.USER_CANCELED) {
                _purchasesUpdatedEvent.emit(SubscriptionUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_update_purchases)))
                analyticsManager.recordException(PurchasesUpdatedException(billingResult.debugMessage))
            }
        }
    }
    private val billingClient = billingClientBuilder
        .setListener(purchasesUpdatedListener)
        .build()
    private val productList = listOf(
        QueryProductDetailsParams.Product.newBuilder()
            .setProductId("com.weathersync.premium")
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
    )
    // call this function before generation and weather fetch
    suspend fun initBillingClient(): IsSubscribed = billingClientInitMutex.withLock {
        if (!billingClient.isReady) {
            subscriptionInfoDatastore.setIsSubscribed(false)
            startConnection()
        }
        return setIsSubscribed()
    }


    private suspend fun setIsSubscribed(): IsSubscribed {
        isSubscribedMutex.withLock {
            val isSubscribed = fetchUpToDateSubscriptionState()
            subscriptionInfoDatastore.setIsSubscribed(isSubscribed)
            return isSubscribed
        }
    }

    // use resume variable since onBillingServiceDisconnected and onBillingSetupFinished could be called at the same time
    // when switching/removing accounts on the device
    private suspend fun startConnection(): IsBillingSetupFinished = suspendCancellableCoroutine { continuation ->
        var resumed = false
        billingClient.startConnection(object: BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                if (!resumed) {
                    resumed = true
                    continuation.resumeWithException(BillingServiceDisconnected("Is billing client ready: ${billingClient.isReady}"))
                }
            }
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (!resumed) {
                    resumed = true
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        continuation.resume(true)
                    } else continuation.resumeWithException(BillingServiceInitException(billingResult.debugMessage))
                }
            }
        })
    }

    fun isSubscribedFlow() = subscriptionInfoDatastore.isSubscribedFlow()

    private suspend fun fetchUpToDateSubscriptionState(): IsSubscribed = suspendCancellableCoroutine { continuation ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                continuation.resume(
                    purchases.any { purchase ->
                        purchase.purchaseState == PurchaseState.PURCHASED
                    })
            } else continuation.resumeWithException(SubscriptionCheckException(billingResult.debugMessage))
        }
    }

    private suspend fun getProductDetails(): List<ProductDetails>? {
        val params = QueryProductDetailsParams.newBuilder()
        params.setProductList(productList)

        return withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params.build())
        }.productDetailsList
    }

    suspend fun processPurchases(): List<SubscriptionDetails>? =
        getProductDetails()?.map { it.toSubscriptionDetails() }

    suspend fun purchase(activity: Activity) {
        val productDetails = getProductDetails() ?: return
        // selects a product with free trial offer first, if its unavailable fallback to the first available product
        val selectedProductDetails = productDetails.firstOrNull { product ->
            product.subscriptionOfferDetails?.any { offer -> offer.containsFreeTrial() } == true
        } ?: productDetails.firstOrNull() ?: return
        val selectedOfferToken = selectedProductDetails.subscriptionOfferDetails
            ?.firstOrNull { offer -> offer.containsFreeTrial() }?.offerToken
            ?: selectedProductDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(selectedProductDetails)
                .setOfferToken(selectedOfferToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == PurchaseState.PURCHASED &&
            !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val acknowledgePurchaseResult = withContext(Dispatchers.IO) {
                billingClient.acknowledgePurchase(acknowledgePurchaseParams)
            }
        }
    }

    private fun ProductDetails.SubscriptionOfferDetails.containsFreeTrial() =
        this.pricingPhases.pricingPhaseList.any { phase ->
            phase.priceAmountMicros == 0L
        }

    private fun ProductDetails.toSubscriptionDetails(): SubscriptionDetails {
        val offers = subscriptionOfferDetails?.map { offer ->
            OfferDetails(
                offerId = offer.offerId,
                pricingPhases = offer.pricingPhases.pricingPhaseList.map { phase ->
                    PricingPhaseDetails(
                        priceCurrencyCode = phase.priceCurrencyCode,
                        priceAmount = phase.priceAmountMicros.toDouble() / 1_000_000,
                        billingPeriod = phase.billingPeriod,
                        recurrenceMode = phase.recurrenceMode,
                        isFreeTrial = phase.priceAmountMicros == 0L && phase.billingPeriod.isNotEmpty()
                    )
                }
            )
        } ?: emptyList()

        return SubscriptionDetails(
            productId = productId,
            title = title,
            description = description,
            offers = offers
        )
    }
}