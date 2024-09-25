package com.weathersync.common.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider

fun getString(@StringRes resId: Int): String =
    ApplicationProvider.getApplicationContext<Context>().getString(resId)

fun SemanticsNodeInteraction.printToLog(
    maxDepth: Int = Int.MAX_VALUE,
) {
    val result = "printToLog:\n" + printToString(maxDepth) + "\n"
    println(result)
}