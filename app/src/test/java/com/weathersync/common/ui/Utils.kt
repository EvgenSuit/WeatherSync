package com.weathersync.common.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import com.weathersync.R
import java.text.SimpleDateFormat
import java.util.Date

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
    expectedNextUpdateDate: Date?) {
    onNodeWithText(
        getString(resId, SimpleDateFormat("HH:mm, dd MMM").format(expectedNextUpdateDate)),
        useUnmergedTree = true).assertIsDisplayed()
}