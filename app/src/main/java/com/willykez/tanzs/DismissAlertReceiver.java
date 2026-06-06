package com.willykez.tanzs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

/**
 * Receives the "Dismiss" action from a price-alert notification and cancels it.
 */
public class DismissAlertReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // The notification auto-cancel flag handles UI removal;
        // this receiver is a hook for future dismiss-side effects
        // (e.g., mark alert as acknowledged in SharedPreferences).
    }
}
