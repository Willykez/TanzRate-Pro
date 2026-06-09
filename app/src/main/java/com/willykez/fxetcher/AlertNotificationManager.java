package com.willykez.fxetcher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.DecimalFormat;

public class AlertNotificationManager {

    public static final String CH_UPDATES = "rate_updates";
    public static final String CH_ALERTS  = "price_alerts";

    public static final int ID_RATE_UPDATE = 1001;

    private static final DecimalFormat FMT = new DecimalFormat("#,##0.00");

    // ═══════════════════════════════════════════════
    // CREATE NOTIFICATION CHANNELS
    // ═══════════════════════════════════════════════
    public static void createChannels(Context ctx) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel updates = new NotificationChannel(
                    CH_UPDATES,
                    "Rate Updates",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationChannel alerts = new NotificationChannel(
                    CH_ALERTS,
                    "Price Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(updates);
            nm.createNotificationChannel(alerts);
        }
    }

    // ═══════════════════════════════════════════════
    // RATE UPDATE NOTIFICATION
    // ═══════════════════════════════════════════════
    public static void postRateUpdate(Context ctx,
                                      Double usd,
                                      Double eur,
                                      Double gbp) {

        String title = "FX Rate Update";

        String body =
                "USD: " + usd + "\n" +
                "EUR: " + eur + "\n" +
                "GBP: " + gbp;

        Bitmap icon = buildUpdateIcon();

        Intent intent = new Intent(ctx, FXetcherApp.class);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CH_UPDATES)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setLargeIcon(icon)
                .setContentTitle(title)
                .setContentText("Tap to view latest rates")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManagerCompat.from(ctx).notify(ID_RATE_UPDATE, nb.build());
    }

    // ═══════════════════════════════════════════════
    // PRICE ALERT NOTIFICATION
    // ═══════════════════════════════════════════════
    public static void postPriceAlert(Context ctx,
                                      String currency,
                                      int condition,
                                      double target,
                                      double current) {

        boolean isAbove = current > target;

        int color = isAbove ? 0xFF4CAF50 : 0xFFF44336;

        String title = currency + " Alert";

        String body = (isAbove ? "Above target: " : "Below target: ")
                + FMT.format(current)
                + " (Target: " + FMT.format(target) + ")";

        Bitmap icon = buildAlertIcon(color);

        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CH_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setLargeIcon(icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setColor(color)
                .setColorized(true);

        NotificationManagerCompat.from(ctx).notify(currency.hashCode(), nb.build());
    }

    // ═══════════════════════════════════════════════
    // ICON BUILDER (SAFE - NO RECYCLE)
    // ═══════════════════════════════════════════════
    private static Bitmap buildUpdateIcon() {

        Bitmap bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setColor(0xFF9C27B0);
        c.drawCircle(100, 100, 90, p);

        return bmp;
    }

    private static Bitmap buildAlertIcon(int color) {

        Bitmap bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setColor(color);
        c.drawCircle(100, 100, 90, p);

        return bmp;
    }
}