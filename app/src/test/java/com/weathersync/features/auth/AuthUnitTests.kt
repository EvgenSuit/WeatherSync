package com.weathersync.features.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.BaseTest
import com.weathersync.common.ui.TextFieldState
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
    fun performManualSignIn_inputIsCorrect_success() = testScope.runTest {
        val email = "some@gmail.com"
        val password = "Password2077$"
        val emailIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Email, TextFieldState(email)))
        val passwordIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Password, TextFieldState(password)))
        viewModel.handleIntent(emailIntent)
        viewModel.handleIntent(passwordIntent)

        assertEquals("", viewModel.uiState.value.fieldsState.email.state.error!!.asString(context))
        assertEquals("", viewModel.uiState.value.fieldsState.password.state.error!!.asString(context))
        viewModel.handleIntent(AuthIntent.ManualAuth)
        advanceUntilIdle()
        verify { auth.signInWithEmailAndPassword(email, password) }
    }
    @Test
    fun performManualSignUp_inputIsCorrect_success() = testScope.runTest {
        val email = "some@gmail.com"
        val password = "Password2077$"
        val emailIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Email, TextFieldState(email)))
        val passwordIntent = AuthIntent.AuthInput(AuthTextFieldState(AuthFieldType.Password, TextFieldState(password)))
        viewModel.handleIntent(emailIntent)
        viewModel.handleIntent(passwordIntent)

        assertEquals("", viewModel.uiState.value.fieldsState.email.state.error!!.asString(context))
        assertEquals("", viewModel.uiState.value.fieldsState.password.state.error!!.asString(context))
        viewModel.handleIntent(AuthIntent.ChangeAuthType(AuthType.SignUp))
        viewModel.handleIntent(AuthIntent.ManualAuth)
        advanceUntilIdle()
        verify { auth.createUserWithEmailAndPassword(email, password) }
    }
}