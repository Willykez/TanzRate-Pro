package com.willykez.fxetcher

import android.app.Application
import com.willykez.fxetcher.notifications.NotificationHelper

class FXetcherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
