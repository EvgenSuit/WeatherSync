package com.weathersync.utils.appReview.data

import androidx.annotation.Keep
import java.util.Date

@Keep
data class RateDialog(
    val firstEntryDate: Date? = null,
    val didShow: Boolean = false
)
