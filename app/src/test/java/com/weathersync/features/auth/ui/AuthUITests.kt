package com.weathersync.features.auth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.R
import com.weathersync.common.auth.invalidEmails
import com.weathersync.common.auth.invalidPasswords
import com.weathersync.common.auth.validEmail
import com.weathersync.common.auth.validPassword
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.auth.BaseAuthRule
import com.weathersync.features.auth.presentation.ui.AuthScreen
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AuthUITests {
    @get: Rule
    val composeRule = createComposeRule()
    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val baseAuthRule = BaseAuthRule()
    private val snackbarScope = TestScope()
    /*@get: Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeRule,
        captureRoot = composeRule.onRoot(),
    )*/

    @Test
    fun `test visibility toggle`() = runTest {
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = baseAuthRule.viewModel, onNavigateToHome = {}) }) {
            val passwordField = onNodeWithTag(getString(R.string.password))
            val passwordVisibleIcon = onNodeWithContentDescription("Password visible")
            val passwordNotVisibleIcon = onNodeWithContentDescription("Password not visible")

            // assert visibility toggle is not displayed when the password text field is blank
            passwordVisibleIcon.assertDoesNotExist()
            passwordNotVisibleIcon.assertDoesNotExist()

            passwordField.performTextReplacement(validPassword)
            passwordNotVisibleIcon.assertIsDisplayed()
            passwordVisibleIcon.assertDoesNotExist()

            // show password
            passwordNotVisibleIcon.performClick()
            passwordVisibleIcon.assertIsDisplayed()
            passwordNotVisibleIcon.assertDoesNotExist()
        }
    }

    @Test
    fun performManualSignIn_inputIsValid_success() = runTest {
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = baseAuthRule.viewModel, onNavigateToHome = {}) }) {
            performAuth(true, this@runTest)
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }
    @Test
    fun performManualSignUp_inputIsValid_success() = runTest {
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = baseAuthRule.viewModel, onNavigateToHome = {}) }) {
            performAuth(false, this@runTest)
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }
    @Test
    fun performManualSignIn_inputIsValid_error() = runTest {
        baseAuthRule.setup(exception = baseAuthRule.testHelper.testException)
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = baseAuthRule.viewModel, onNavigateToHome = {}) }) {
            performAuth(true, this@runTest)
            assertSnackbarTextEquals(R.string.auth_error, snackbarScope)
            assertEquals(baseAuthRule.testHelper.testException, baseAuthRule.testHelper.exceptionSlot.captured)
        }
    }
    @Test
    fun performManualSignUp_inputIsValid_error() = runTest {
        baseAuthRule.setup(exception = baseAuthRule.testHelper.testException)
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = baseAuthRule.viewModel, onNavigateToHome = {}) }) {
            performAuth(false, this@runTest)
            assertSnackbarTextEquals(R.string.auth_error, snackbarScope)
            assertEquals(baseAuthRule.testHelper.testException, baseAuthRule.testHelper.exceptionSlot.captured)
        }
    }
    @Test
    fun performInvalidInput_errorsAreNotEmpty() = runTest {
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = baseAuthRule.viewModel, onNavigateToHome = {}) }) {
            invalidEmails.forEach { email ->
                if (email.isNotEmpty()) {
                    onNodeWithTag(getString(R.string.email), useUnmergedTree = true).performTextReplacement(email)
                    onNodeWithTag("${getString(R.string.email)} error", useUnmergedTree = true).assertExists()
                }
            }
            invalidPasswords.forEach { password ->
                if (password.isNotEmpty()) {
                    onNodeWithTag(getString(R.string.password), useUnmergedTree = true).performTextReplacement(password)
                    onNodeWithTag("${getString(R.string.password)} error", useUnmergedTree = true).assertExists()
                }
            }
        }
    }


    private fun ComposeContentTestRule.performAuth(signIn: Boolean, testScope: TestScope) {
        if (!signIn) onNodeWithText(getString(R.string.go_to_sign_up), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed().performClick()
        val emailField = onNodeWithTag(getString(R.string.email))
        val passwordField = onNodeWithTag(getString(R.string.password))
        emailField.performTextReplacement(validEmail)
        passwordField.performTextReplacement(validPassword)

        onNodeWithText(getString(if (signIn) R.string.sign_in else R.string.sign_up)).assertIsEnabled().performClick().assertIsNotEnabled()
        listOf(emailField, passwordField).forEach { it.assertIsNotEnabled() }
        onNodeWithText(getString(if (signIn) R.string.sign_in_with_google else R.string.sign_up_with_google)).assertIsNotEnabled()

        testScope.advanceUntilIdle()
        verify { baseAuthRule.auth.apply {
            if (signIn) signInWithEmailAndPassword(validEmail, validPassword)
            else createUserWithEmailAndPassword(validEmail, validPassword)
        } }
    }
}