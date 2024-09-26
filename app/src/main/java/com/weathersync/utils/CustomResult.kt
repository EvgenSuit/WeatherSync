package com.weathersync.utils

import androidx.annotation.StringRes
import com.weathersync.common.ui.UIText

sealed class CustomResult(val data: UIText = UIText.Empty) {
    data object None: CustomResult()
    data object InProgress: CustomResult()
    data class Success(@StringRes val message: Int? = null, val args: List<Any> = emptyList()):
        CustomResult(data = if (message != null) UIText.StringResource(message, args) else UIText.Empty)
    data object Error: CustomResult()

}

fun CustomResult.isInProgress() = this is CustomResult.InProgress
fun CustomResult.isSuccess() = this is CustomResult.Success
fun CustomResult.isError() = this is CustomResult.Error //CustomResult.DynamicError || this is CustomResult.ResourceError

fun Exception.toStringIfMessageIsNull() = message ?: this.toString()