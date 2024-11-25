package com.weathersync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.weathersync.common.ui.LocalSnackbarController
import com.weathersync.common.ui.SnackbarController
import com.weathersync.features.navigation.presentation.ui.NavManager
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.ui.theme.WeatherSyncTheme
import com.weathersync.utils.AdLoadError
import com.weathersync.utils.AdShowError
import com.weathersync.utils.AnalyticsManager
import com.weathersync.utils.ads.AdBanner
import com.weathersync.utils.ads.AdsDatastoreManager
import com.weathersync.utils.appReview.AppReviewManager
import com.weathersync.utils.subscription.SubscriptionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val themeManager: ThemeManager by inject()
    private val subscriptionManager: SubscriptionManager by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val appReviewManager: AppReviewManager by inject()

    private val didCallBillingInitMethod = mutableStateOf(false)

    private val adsDatastoreManager: AdsDatastoreManager by inject()
    private var mInterstitialAd = mutableStateOf<InterstitialAd?>(null)

    private fun initBillingClient() = lifecycleScope.launch {
        try {
            subscriptionManager.initBillingClient()
        } catch (e: Exception) {
            analyticsManager.recordException(e)
        } finally {
            didCallBillingInitMethod.value = true
        }
    }

    private fun requestReviewFlow() {
        lifecycleScope.launch {
            try {
                appReviewManager.requestReviewFlow(this@MainActivity)
            } catch (e: Exception) {
                analyticsManager.recordException(e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // https://developer.android.com/google/play/billing/integrate#fetch
        initBillingClient()
    }

    override fun onDestroy() {
        super.onDestroy()
        AdBanner.destroyAdViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadInterstitialAd()
        AdBanner.preloadAdPromoViews(applicationContext)

        requestReviewFlow()

        installSplashScreen().setKeepOnScreenCondition {
            !didCallBillingInitMethod.value
        }
        // keep in mind that because of enableEdgeToEdge interstitial ads might not take full screen height
        enableEdgeToEdge()

        val initTheme = runBlocking { themeManager.themeFlow(true).first() }
        setContent {
            val isThemeDark by themeManager.themeFlow(true).collectAsState(initial = initTheme)
            window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

            val showAd by adsDatastoreManager.showInterstitialAdFlow().collectAsStateWithLifecycle(initialValue = false)
            val navController = rememberNavController()
            val snackbarHostState = remember { SnackbarHostState() }
            val snackbarController by remember {
                mutableStateOf(SnackbarController(
                    context = applicationContext,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = lifecycleScope))
            }
            WeatherSyncTheme(isThemeDark) {
                CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
                    NavManager(
                        navController = navController,
                        activity = this
                    )
                }
            }

            LaunchedEffect(showAd, mInterstitialAd.value) {
                if (showAd && mInterstitialAd.value != null) {
                    this@MainActivity.showInterstitial(
                        interstitialAd = mInterstitialAd.value,
                        onDismissed = {
                            runBlocking { adsDatastoreManager.setShowInterstitialAd(false) }
                        })
                }
            }
        }
    }
    private fun loadInterstitialAd() {
        // uncomment this line to test ads integration
        if (BuildConfig.DEBUG) return
        val adRequest = AdRequest.Builder().build()
        val adUnitId = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712"
        else BuildConfig.INTERSTITIAL_AD_UNIT_ID
        InterstitialAd.load(this, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                mInterstitialAd.value = null
                analyticsManager.recordException(AdLoadError(error.message))
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd.value = interstitialAd
            }
        })
    }
    private fun showInterstitial(interstitialAd: InterstitialAd?,
                                          onDismissed: () -> Unit) {
        interstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                mInterstitialAd.value = null
                analyticsManager.recordException(AdShowError(error.message))
            }

            override fun onAdDismissedFullScreenContent() {
                mInterstitialAd.value = null
                onDismissed()
            }
        }
        interstitialAd?.show(this)
    }
}


@Composable
fun Modifier.clearFocusOnNonButtonClick(focusManager: FocusManager) =
    this.pointerInput(Unit) {
        // Clear focus when a click event is triggered (text fields and buttons are not included)
        // focus will still be cleared when a text field is disabled
        awaitEachGesture {
            val downEvent = awaitFirstDown(pass = PointerEventPass.Initial)
            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (upEvent != null && !downEvent.isConsumed) focusManager.clearFocus(true)
        }
    }