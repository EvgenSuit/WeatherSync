package com.weathersync.features.auth.unit

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.BaseTest
import com.weathersync.common.auth.invalidEmails
import com.weathersync.common.auth.invalidPasswords
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.validEmail
import com.weathersync.common.auth.validPassword
import com.weathersync.common.ui.TextFieldState
import com.weathersync.features.auth.GoogleAuthRepository
import com.weathersync.features.auth.RegularAuthRepository
import com.weathersync.features.auth.ui.AuthFieldType
import com.weathersync.features.auth.ui.AuthIntent
import com.weathersync.features.auth.ui.AuthTextFieldState
import com.weathersync.features.auth.ui.AuthType
import com.weathersync.features.auth.ui.AuthViewModel
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthUnitTests: BaseTest() {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var viewModel: AuthViewModel

    private fun setup(
        exception: Exception? = null
    ) {
        auth = mockAuth(exception = exception)
        val regularAuthRepository = RegularAuthRepository(auth)
        val googleAuthRepository = mockk<GoogleAuthRepository>()
        viewModel = AuthViewModel(
            regularAuthRepository = regularAuthRepository,
            googleAuthRepository = googleAuthRepository,
            crashlyticsManager = crashlyticsManager,
            coroutineScopeProvider = coroutineScopeProvider
        )
    }
    @Before
    fun before() {
        setup()
    }

    @Test
    fun performManualSignIn_inputIsValid_success() = testScope.runTest {
        performManualAuth(AuthType.SignIn)
        assertFalse(crashlyticsExceptionSlot.isCaptured)
    }
    @Test
    fun performManualSignUp_inputIsValid_success() = testScope.runTest {
        performManualAuth(AuthType.SignUp)
        assertFalse(crashlyticsExceptionSlot.isCaptured)
    }
    @Test
    fun performManualSignIn_inputIsValid_error() = testScope.runTest {
        setup(exception = exception)
        performManualAuth(AuthType.SignIn)
        assertEquals(exception.message, crashlyticsExceptionSlot.captured.message)
    }
    @Test
    fun performManualSignUp_inputIsValid_error() = testScope.runTest {
        setup(exception = exception)
        performManualAuth(AuthType.SignUp)
        assertEquals(exception.message, crashlyticsExceptionSlot.captured.message)
    }
    @Test
    fun performInvalidInput_errorsAreNotEmpty() {
        invalidEmails.forEach { email ->
            val emailIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Email, TextFieldState(email)))
            viewModel.handleIntent(emailIntent)
            assert(viewModel.uiState.value.fieldsState.email.state.error!!.asString(context).isNotEmpty())
        }
        invalidPasswords.forEach { password ->
            val passwordIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Password, TextFieldState(password)))
            viewModel.handleIntent(passwordIntent)
            assert(viewModel.uiState.value.fieldsState.password.state.error!!.asString(context).isNotEmpty())
        }
    }
    private fun performManualAuth(type: AuthType) {
        viewModel.handleIntent(AuthIntent.ChangeAuthType(type))
        val emailIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Email, TextFieldState(validEmail)))
        val passwordIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Password, TextFieldState(validPassword)))
        viewModel.handleIntent(emailIntent)
        viewModel.handleIntent(passwordIntent)

        assertEquals("", viewModel.uiState.value.fieldsState.email.state.error!!.asString(context))
        assertEquals("", viewModel.uiState.value.fieldsState.password.state.error!!.asString(context))
        viewModel.handleIntent(AuthIntent.ManualAuth)
        testScope.advanceUntilIdle()
        verify { auth.apply { if (type == AuthType.SignUp) createUserWithEmailAndPassword(validEmail, validPassword)
        else signInWithEmailAndPassword(validEmail, validPassword) } }
    }
}