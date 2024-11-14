package com.weathersync.features.subscription.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.R
import com.weathersync.common.ui.UIText
import com.weathersync.features.subscription.SubscriptionInfoRepository
import com.weathersync.features.subscription.presentation.ui.SubscriptionScreenIntent
import com.weathersync.ui.SubscriptionUIEvent
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.CustomResult
import com.weathersync.utils.subscription.data.SubscriptionDetails
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionInfoViewModel(
    private val subscriptionInfoRepository: SubscriptionInfoRepository,
    private val analyticsManager: AnalyticsManager
): ViewModel() {
    private val _uiEvents = MutableSharedFlow<SubscriptionUIEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _uiState = MutableStateFlow(SubscriptionInfoUIState())
    val uiState = _uiState.asStateFlow()
    val purchasesUpdatedEvent = subscriptionInfoRepository.purchasesUpdatedEvent

    init {
        viewModelScope.launch {
            subscriptionInfoRepository.isSubscribedFlow().collectLatest {
                if (it != null && it) _uiEvents.emit(SubscriptionUIEvent.NavigateUp)
            }
        }
    }

    fun handleIntent(intent: SubscriptionScreenIntent) {
        when (intent) {
            is SubscriptionScreenIntent.FetchSubscriptionDetails -> fetchSubscriptionDetails()
            is SubscriptionScreenIntent.Purchase -> purchase(intent.activity)
        }
    }

    private fun fetchSubscriptionDetails() {
        updateInfoFetchResult(CustomResult.InProgress)
        viewModelScope.launch {
            try {
                // make sure billing is initialized correctly
                subscriptionInfoRepository.isBillingSetupFinished()
                val details = subscriptionInfoRepository.getSubscriptionDetails()
                _uiState.update { it.copy(subscriptionDetails = details) }
                updateInfoFetchResult(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvents.emit(SubscriptionUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_fetch_subscription_info)))
                analyticsManager.recordException(e)
                updateInfoFetchResult(CustomResult.Error)
            }
        }
    }

    private fun purchase(activity: Activity) {
        updatePurchaseResult(CustomResult.InProgress)
        viewModelScope.launch {
            try {
                subscriptionInfoRepository.purchase(activity)
                updatePurchaseResult(CustomResult.Success)
            } catch (e: Exception) {
                _uiEvents.emit(SubscriptionUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_complete_purchase)))
                analyticsManager.recordException(e)
                updatePurchaseResult(CustomResult.Error)
            }
        }
    }

    private fun updateInfoFetchResult(result: CustomResult) =
        _uiState.update { it.copy(infoFetchResult = result) }
    private fun updatePurchaseResult(result: CustomResult) =
        _uiState.update { it.copy(purchaseResult = result) }

}

data class SubscriptionInfoUIState(
    val isSubscribed: Boolean? = null,
    val subscriptionDetails: List<SubscriptionDetails>? = null,
    val infoFetchResult: CustomResult = CustomResult.None,
    val purchaseResult: CustomResult = CustomResult.None
)