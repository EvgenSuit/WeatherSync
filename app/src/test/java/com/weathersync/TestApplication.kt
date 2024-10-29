package com.weathersync

import android.app.Application

// needed to run tests containing koin since they default to the application defined in the manifest
class TestApplication: Application()
