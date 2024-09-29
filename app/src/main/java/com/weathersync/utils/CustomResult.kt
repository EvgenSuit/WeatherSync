package com.weathersync.utils

import androidx.annotation.StringRes
import com.weathersync.common.ui.UIText

sealed class CustomResult {
    data object None: CustomResult()
    data object InProgress: CustomResult()
    data object Success: CustomResult()
    data object Error: CustomResult()

}

fun CustomResult.isInProgress() = this is CustomResult.InProgress
fun CustomResult.isSuccess() = this is CustomResult.Success
fun CustomResult.isError() = this is CustomResult.Error //CustomResult.DynamicError || this is CustomResult.ResourceError

fun Exception.toStringIfMessageIsNull() = message ?: this.toString()