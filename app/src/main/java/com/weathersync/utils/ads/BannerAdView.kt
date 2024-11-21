package com.weathersync.utils.ads

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.weathersync.BuildConfig

sealed class AdBannerType(val name: String) {
    data object Home: AdBannerType("HomeAd")
    data object ActivityPlanning: AdBannerType("ActivityPlanningAd")
}

object AdBanner {
    private var homePromoAdView: AdView? = null
    private var activityPlanningAdView: AdView? = null

    private fun preloadAdPromoView(context: Context, adType: AdBannerType) {
        val unitId = when (adType) {
            is AdBannerType.Home ->  if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741"
            else BuildConfig.HOME_PROMO_AD_UNIT_ID
            is AdBannerType.ActivityPlanning -> if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741"
            else BuildConfig.ACTIVITY_PLANNING_PROMO_AD_UNIT_ID
        }
        val adView = AdView(context.applicationContext).apply {
            adUnitId = unitId
            setAdSize(AdSize.BANNER)
            loadAd(AdRequest.Builder().build())
        }
        when (adType) {
            is AdBannerType.Home -> {
                if (homePromoAdView == null) {
                    homePromoAdView = adView
                }
            }
            is AdBannerType.ActivityPlanning -> {
                if (activityPlanningAdView == null) {
                    activityPlanningAdView = adView
                }
            }
        }
    }

    fun preloadAdPromoViews(context: Context) {
        preloadAdPromoView(context, AdBannerType.Home)
        preloadAdPromoView(context, AdBannerType.ActivityPlanning)
    }

    fun getAdPromo(adBannerType: AdBannerType) = when(adBannerType) {
        is AdBannerType.Home -> homePromoAdView
        is AdBannerType.ActivityPlanning -> activityPlanningAdView
    }

    fun destroyAdViews() {
        homePromoAdView?.destroy()
        homePromoAdView = null
        activityPlanningAdView?.destroy()
        activityPlanningAdView = null
    }
}

@Composable
fun BannerAdView(adBannerType: AdBannerType) {
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.onBackground.copy(0.7f)))
    } else {
        val adView = remember { AdBanner.getAdPromo(adBannerType) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .testTag(adBannerType.name)
        ) {
            if (adView != null) {
                AndroidView(
                    factory = { adView },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}