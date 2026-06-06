package com.willykez.tanzs;

import android.app.Application;
import androidx.multidex.MultiDexApplication;

/**
 * Application class for TanzRate Pro.
 * Initialises notification channels on startup.
 */
public class TanzRateApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // Create all notification channels once at app start
        AlertNotificationManager.createChannels(this);
    }
}
