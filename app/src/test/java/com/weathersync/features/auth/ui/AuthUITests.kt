package com.weathersync.features.auth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.R
import com.weathersync.common.BaseTest
import com.weathersync.common.auth.invalidEmails
import com.weathersync.common.auth.invalidPasswords
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.validEmail
import com.weathersync.common.auth.validPassword
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.features.auth.GoogleAuthRepository
import com.weathersync.features.auth.RegularAuthRepository
import com.weathersync.features.auth.presentation.AuthViewModel
import com.weathersync.features.auth.presentation.ui.AuthScreen
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AuthUITests: BaseTest() {
    private lateinit var viewModel: AuthViewModel
    @get: Rule
    val composeRule = createComposeRule()
    /*@get: Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeRule,
        captureRoot = composeRule.onRoot(),
    )*/

    private fun setup(
        exception: Exception? = null
    ) {
        auth = mockAuth(exception = exception)
        val regularAuthRepository = RegularAuthRepository(auth)
        val googleAuthRepository = mockk<GoogleAuthRepository>()
        viewModel = AuthViewModel(
            regularAuthRepository = regularAuthRepository,
            googleAuthRepository = googleAuthRepository,
            crashlyticsManager = crashlyticsManager
        )
    }
    @Before
    fun before() {
        setup()
    }
    @Test
    fun performManualSignIn_inputIsValid_success() = runTest {
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = viewModel, onNavigateToHome = {}) }) {
            performAuth(true, this@runTest)
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }
    @Test
    fun performManualSignUp_inputIsValid_success() = runTest {
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = viewModel, onNavigateToHome = {}) }) {
            performAuth(false, this@runTest)
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }
    @Test
    fun performManualSignIn_inputIsValid_error() = runTest {
        setup(exception = exception)
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = viewModel, onNavigateToHome = {}) }) {
            performAuth(true, this@runTest)
            assertSnackbarTextEquals(R.string.auth_error, snackbarScope)
            assertEquals(exception.message, crashlyticsExceptionSlot.captured.message)
        }
    }
    @Test
    fun performManualSignUp_inputIsValid_error() = runTest {
        setup(exception = exception)
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = viewModel, onNavigateToHome = {}) }) {
            performAuth(false, this@runTest)
            assertSnackbarTextEquals(R.string.auth_error, snackbarScope)
            assertEquals(exception.message, crashlyticsExceptionSlot.captured.message)
        }
    }
    @Test
    fun performInvalidInput_errorsAreNotEmpty() = runTest {
        setContentWithSnackbar(composeRule, snackbarScope,
            uiContent = { AuthScreen(viewModel = viewModel, onNavigateToHome = {}) }) {
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
        verify { auth.apply {
            if (signIn) signInWithEmailAndPassword(validEmail, validPassword)
            else createUserWithEmailAndPassword(validEmail, validPassword)
        } }
    }
}