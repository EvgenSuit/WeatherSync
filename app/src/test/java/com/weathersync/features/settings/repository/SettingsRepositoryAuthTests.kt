package com.weathersync.features.settings.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.features.settings.SettingsBaseRule
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryAuthTests {
    @get: Rule
    val settingsBaseRule = SettingsBaseRule()

    @Test
    fun signOut_isUserNull() {
        settingsBaseRule.settingsRepository.signOut()
        assertEquals(null, settingsBaseRule.auth.currentUser)
        verify { settingsBaseRule.auth.signOut() }
    }
}