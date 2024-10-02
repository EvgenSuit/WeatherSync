package com.weathersync.features.activityPlanning

import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ActivityPlanningBaseRule: TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
    }
}