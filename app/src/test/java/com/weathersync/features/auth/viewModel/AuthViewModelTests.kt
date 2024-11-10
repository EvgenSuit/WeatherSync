package com.weathersync.features.auth.viewModel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.auth.invalidEmails
import com.weathersync.common.auth.invalidPasswords
import com.weathersync.common.auth.validEmail
import com.weathersync.common.auth.validPassword
import com.weathersync.common.ui.TextFieldState
import com.weathersync.common.MainDispatcherRule
import com.weathersync.features.auth.BaseAuthRule
import com.weathersync.features.auth.presentation.AuthIntent
import com.weathersync.features.auth.presentation.AuthType
import com.weathersync.features.auth.presentation.ui.AuthFieldType
import com.weathersync.features.auth.presentation.ui.AuthTextFieldState
import com.weathersync.utils.FirebaseEvent
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthViewModelTests {
    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val baseAuthRule = BaseAuthRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun performManualSignIn_inputIsValid_success() = runTest {
        performManualAuth(AuthType.SignIn)
        assertFalse(baseAuthRule.testHelper.exceptionSlot.isCaptured)
        baseAuthRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.MANUAL_SIGN_IN, false)
    }
    @Test
    fun performManualSignUp_inputIsValid_success() = runTest {
        performManualAuth(AuthType.SignUp)
        assertFalse(baseAuthRule.testHelper.exceptionSlot.isCaptured)
        baseAuthRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.MANUAL_SIGN_UP, false)
    }
    @Test
    fun performManualSignIn_inputIsValid_error() = runTest {
        baseAuthRule.setup(exception = baseAuthRule.testHelper.testException)
        performManualAuth(AuthType.SignIn)
        assertEquals(baseAuthRule.testHelper.testException.message, baseAuthRule.testHelper.exceptionSlot.captured.message)
    }
    @Test
    fun performManualSignUp_inputIsValid_error() = runTest {
        baseAuthRule.setup(exception = baseAuthRule.testHelper.testException)
        performManualAuth(AuthType.SignUp)
        assertEquals(baseAuthRule.testHelper.testException.message, baseAuthRule.testHelper.exceptionSlot.captured.message)
    }
    @Test
    fun performInvalidInput_errorsAreNotEmpty() {
        invalidEmails.forEach { email ->
            val emailIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Email, TextFieldState(email)))
            baseAuthRule.viewModel.handleIntent(emailIntent)
            assert(baseAuthRule.viewModel.uiState.value.fieldsState.email.state.error!!.asString(context).isNotEmpty())
        }
        invalidPasswords.forEach { password ->
            val passwordIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Password, TextFieldState(password)))
            baseAuthRule.viewModel.handleIntent(passwordIntent)
            assert(baseAuthRule.viewModel.uiState.value.fieldsState.password.state.error!!.asString(context).isNotEmpty())
        }
    }
    private fun TestScope.performManualAuth(type: AuthType) {
        baseAuthRule.viewModel.handleIntent(AuthIntent.ChangeAuthType(type))
        val emailIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Email, TextFieldState(validEmail)))
        val passwordIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Password, TextFieldState(validPassword)))
        baseAuthRule.viewModel.handleIntent(emailIntent)
        baseAuthRule.viewModel.handleIntent(passwordIntent)

        assertEquals("", baseAuthRule.viewModel.uiState.value.fieldsState.email.state.error!!.asString(context))
        assertEquals("", baseAuthRule.viewModel.uiState.value.fieldsState.password.state.error!!.asString(context))
        baseAuthRule.viewModel.handleIntent(AuthIntent.ManualAuth)
        advanceUntilIdle()
        verify { baseAuthRule.auth.apply { if (type == AuthType.SignUp) createUserWithEmailAndPassword(validEmail, validPassword)
        else signInWithEmailAndPassword(validEmail, validPassword) } }
    }
}