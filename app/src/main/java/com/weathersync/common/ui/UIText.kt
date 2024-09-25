package com.weathersync.common.ui

import android.content.Context
import androidx.annotation.StringRes

sealed class UIText {
    abstract fun asString(context: Context): String

    data object Empty: UIText() {
        override fun asString(context: Context): String = ""
    }
    data class DynamicString(val value: String): UIText() {
        override fun asString(context: Context): String = value
    }
    data class StringResource(
        @StringRes val resId: Int,
        val args: Any = listOf<Any>()
    ): UIText() {
        override fun asString(context: Context): String = context.getString(resId, *arrayOf(args))
    }
}