package com.weathersync.utils

import androidx.annotation.StringRes
import com.weathersync.common.ui.UIText

sealed class CustomResult(val data: UIText = UIText.Empty) {
    data object None: CustomResult()
    data object InProgress: CustomResult()
    data class Success(@StringRes val message: Int? = null, val args: List<Any> = emptyList()):
        CustomResult(data = if (message != null) UIText.StringResource(message, args) else UIText.Empty)
    data class DynamicError(val message: String): CustomResult(UIText.DynamicString(message))
    data class ResourceError(@StringRes val message: Int, val args: List<Any> = emptyList()):
        CustomResult(UIText.StringResource(message, args))

}

fun CustomResult.isInProgress() = this is CustomResult.InProgress
fun CustomResult.isSuccess() = this is CustomResult.Success
fun CustomResult.isError() = this is CustomResult.DynamicError || this is CustomResult.ResourceError