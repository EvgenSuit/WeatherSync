package com.weathersync.common.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getString(@StringRes resId: Int,
              vararg args: Any): String =
    ApplicationProvider.getApplicationContext<Context>().getString(resId, *args)

fun SemanticsNodeInteraction.printToLog(
    maxDepth: Int = Int.MAX_VALUE,
) {
    val result = "printToLog:\n" + printToString(maxDepth) + "\n"
    println(result)
}
fun ComposeContentTestRule.assertDisplayedLimitIsCorrect(
    @StringRes resId: Int,
    expectedNextUpdateDate: Date?,
    locale: Locale) {
    val timePattern = (DateFormat.getTimeInstance(DateFormat.SHORT, locale) as SimpleDateFormat).toPattern()
    onNodeWithText(
        getString(resId, SimpleDateFormat("$timePattern, dd MMM", locale).format(expectedNextUpdateDate)),
        useUnmergedTree = true).performScrollTo().assertIsDisplayed()
}