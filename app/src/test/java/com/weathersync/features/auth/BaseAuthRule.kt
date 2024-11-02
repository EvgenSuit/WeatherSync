package com.weathersync.features.auth

import com.google.firebase.auth.FirebaseAuth
import com.weathersync.common.TestHelper
import com.weathersync.common.auth.mockAuth
import com.weathersync.features.auth.presentation.AuthViewModel
import io.mockk.mockk
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class BaseAuthRule: TestWatcher() {
    val testHelper = TestHelper()
    lateinit var auth: FirebaseAuth
    lateinit var viewModel: AuthViewModel

    fun setup(
        exception: Exception? = null
    ) {
        auth = mockAuth(exception = exception)
        val regularAuthRepository = RegularAuthRepository(auth)
        val googleAuthRepository = mockk<GoogleAuthRepository>()
        viewModel = AuthViewModel(
            regularAuthRepository = regularAuthRepository,
            googleAuthRepository = googleAuthRepository,
            crashlyticsManager = testHelper.crashlyticsManager
        )
    }
    override fun starting(description: Description?) {
        setup()
    }
}