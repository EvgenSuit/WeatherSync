package com.weathersync.common.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID

//In-memory DataStore set up specifically for testing to prevent any unintended file-based persistence across tests
fun createInMemoryDataStore(): DataStore<Preferences> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return PreferenceDataStoreFactory.create(
        produceFile = { File(context.cacheDir, "${UUID.randomUUID()}_preferences.preferences_pb") }
    )
}