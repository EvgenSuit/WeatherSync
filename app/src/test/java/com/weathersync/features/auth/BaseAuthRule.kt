package com.weathersync.features.auth

import com.google.firebase.auth.FirebaseAuth
import com.weathersync.common.TestHelper
import com.weathersync.common.auth.mockAuth
import com.weathersync.features.auth.domain.GoogleAuthRepository
import com.weathersync.features.auth.domain.RegularAuthRepository
import com.weathersync.features.auth.presentation.AuthViewModel
import io.mockk.mockk
import io.mockk.unmockkAll
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
            analyticsManager = testHelper.getAnalyticsManager(mockk(relaxed = true))
        )
    }
    override fun starting(description: Description?) {
        unmockkAll()
        setup()
    }
}