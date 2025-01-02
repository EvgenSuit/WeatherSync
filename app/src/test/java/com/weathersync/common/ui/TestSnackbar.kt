package com.weathersync.common.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert

fun setContentWithSnackbar(
    composeRule: ComposeContentTestRule,
    snackbarScope: CoroutineScope,
    uiContent: @Composable () -> Unit,
    testContent: ComposeContentTestRule .() -> Unit
) {
    composeRule.apply {
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current
            val snackbarController by remember {
                mutableStateOf(SnackbarController(
                    context = context,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = snackbarScope
                ))
            }
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) {
                    CustomSnackbar(snackbarHostState = snackbarHostState, onDismiss = { snackbarHostState.currentSnackbarData?.dismiss() })
                } }, modifier = Modifier.fillMaxSize()) { padding ->
                Box(modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(WindowInsets.ime)) {
                    CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
                        uiContent()
                    }
                }
            }
        }
        testContent()
    }
}

fun ComposeContentTestRule.assertSnackbarTextEquals(
    @StringRes resId: Int,
    snackbarScope: TestScope,
    vararg args: Any,
) {
    waitForIdle()
    snackbarScope.advanceUntilIdle()
    onNodeWithTag("Snackbar", useUnmergedTree = true).assertIsDisplayed()
    onNodeWithTag("Snackbar text: ${getString(resId, *args)}", useUnmergedTree = true).assertIsDisplayed()
}
fun ComposeContentTestRule.assertSnackbarIsNotDisplayed(
    snackbarScope: TestScope
) {
    waitForIdle()
    snackbarScope.advanceUntilIdle()
    onNodeWithTag("Snackbar", useUnmergedTree = true).assertIsNotDisplayed()
}