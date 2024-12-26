package com.weathersync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //loadInterstitialAd()
        requestReviewFlow()

        installSplashScreen().setKeepOnScreenCondition {
            !didCallBillingInitMethod.value
        }
        // keep in mind that because of enableEdgeToEdge interstitial ads might not take full screen height
        enableEdgeToEdge()

        setContent {
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
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val initTheme = runBlocking { themeManager.themeFlow(isDarkByDefault = isSystemInDarkTheme).first() }
            val isThemeDark by themeManager.themeFlow(isDarkByDefault = isSystemInDarkTheme).collectAsState(initial = initTheme)
            WeatherSyncTheme(isThemeDark) {
                CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
                    NavManager(
                        navController = navController,
                        activity = this
                    )
                }
            }
            LaunchedEffect(showAd, mInterstitialAd.value) {
                //if (showAd && mInterstitialAd.value != null) {
                    /*this@MainActivity.showInterstitial(
                        interstitialAd = mInterstitialAd.value,
                        onDismissed = {
                            runBlocking { adsDatastoreManager.setShowInterstitialAd(false) }
                        })*/
                //}
            }
        }
    }
    private fun loadInterstitialAd() {
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(listOf("F6081B2A5942B9D8C33E724EDB0DC022")).build())
        val adRequest = AdRequest.Builder()
            .build()
        val adUnitId = "ca-app-pub-5748792985583679/8435787622"
        InterstitialAd.load(this, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                mInterstitialAd.value = null
                println(error)
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
                println(error)
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