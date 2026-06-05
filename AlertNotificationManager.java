package com.willykez.tanzs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Builds and posts rich, branded price-alert notifications.
 *
 * Channels created:
 *   • tzs_alerts   – HIGH priority   (price threshold hits)
 *   • tzs_updates  – LOW  priority   (periodic rate refresh summary)
 *   • tzs_widget   – MIN  priority   (silent widget-refresh pings)
 */
public class AlertNotificationManager {

    // ── Channel IDs ───────────────────────────────────────────────────────────
    public static final String CH_ALERTS  = "tzs_alerts";
    public static final String CH_UPDATES = "tzs_updates";
    public static final String CH_WIDGET  = "tzs_widget";

    // ── Notification IDs ──────────────────────────────────────────────────────
    private static final int ID_UPDATE_BASE = 10_000;
    private static int alertIdCounter = 20_000;

    // ── Static metadata reused from main app ──────────────────────────────────
    private static final HashMap<String,String> FLAGS   = new HashMap<>();
    private static final HashMap<String,String> NAMES   = new HashMap<>();
    static {
        FLAGS.put("USD","🇺🇸"); FLAGS.put("EUR","🇪🇺"); FLAGS.put("GBP","🇬🇧");
        FLAGS.put("JPY","🇯🇵"); FLAGS.put("CNY","🇨🇳"); FLAGS.put("INR","🇮🇳");
        FLAGS.put("AED","🇦🇪"); FLAGS.put("ZAR","🇿🇦"); FLAGS.put("KES","🇰🇪");
        FLAGS.put("TZS","🇹🇿"); FLAGS.put("UGX","🇺🇬"); FLAGS.put("RWF","🇷🇼");
        FLAGS.put("XAU","💰");   FLAGS.put("XAG","💎");

        NAMES.put("USD","US Dollar");     NAMES.put("EUR","Euro");
        NAMES.put("GBP","British Pound"); NAMES.put("JPY","Japanese Yen");
        NAMES.put("CNY","Chinese Yuan");  NAMES.put("INR","Indian Rupee");
        NAMES.put("AED","UAE Dirham");    NAMES.put("ZAR","S.A. Rand");
        NAMES.put("KES","Kenyan Shilling");NAMES.put("TZS","Tanzanian Shilling");
        NAMES.put("UGX","Ugandan Shilling");NAMES.put("RWF","Rwandan Franc");
        NAMES.put("XAU","Gold (oz)");     NAMES.put("XAG","Silver (oz)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    /** Call once in Application onCreate or Activity onCreate. */
    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        // Price alert channel — vibrates, full heads-up
        NotificationChannel alerts = new NotificationChannel(
                CH_ALERTS, "Price Alerts", NotificationManager.IMPORTANCE_HIGH);
        alerts.setDescription("Fires when a currency rate crosses your target");
        alerts.enableVibration(true);
        alerts.setVibrationPattern(new long[]{0, 250, 150, 250});
        alerts.enableLights(true);
        alerts.setLightColor(Color.parseColor("#FFD700"));
        nm.createNotificationChannel(alerts);

        // Rate-update summary — silent ticker
        NotificationChannel updates = new NotificationChannel(
                CH_UPDATES, "Rate Updates", NotificationManager.IMPORTANCE_LOW);
        updates.setDescription("Background rate refresh summaries");
        nm.createNotificationChannel(updates);

        // Widget silent ping (no sound/vibration)
        NotificationChannel widget = new NotificationChannel(
                CH_WIDGET, "Widget Refresh", NotificationManager.IMPORTANCE_MIN);
        widget.setDescription("Internal pings for widget data refresh");
        widget.setShowBadge(false);
        nm.createNotificationChannel(widget);
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Post a rich price-alert notification.
     *
     * @param currency   3-letter code e.g. "USD"
     * @param condition  0 = rose above, 1 = fell below
     * @param target     target rate in TZS
     * @param current    current rate in TZS
     */
    public static void postPriceAlert(Context ctx,
                                       String currency,
                                       int    condition,
                                       double target,
                                       double current) {
        DecimalFormat fmt = new DecimalFormat("#,##0.00");
        String flag    = FLAGS.getOrDefault(currency, "💱");
        String name    = NAMES.getOrDefault(currency, currency);
        String condStr = condition == 0 ? "rose above" : "fell below";
        String dirEmoji= condition == 0 ? "📈" : "📉";

        String title = dirEmoji + "  " + flag + " " + currency + " Alert Triggered!";
        String body  = name + " just " + condStr + " your target of "
                     + fmt.format(target) + " TZS\n"
                     + "Current rate:  " + fmt.format(current) + " TZS";

        // ── Tap → open app ────────────────────────────────────────────────────
        Intent openApp = new Intent(ctx, TanzaniaForexApp.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openApp.putExtra("startTab", 2); // Markets tab
        PendingIntent tapPi = PendingIntent.getActivity(ctx, alertIdCounter,
                openApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ── Dismiss action ────────────────────────────────────────────────────
        PendingIntent dismissPi = PendingIntent.getBroadcast(ctx, alertIdCounter + 1,
                new Intent("com.willykez.tanzs.DISMISS_ALERT"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ── Build branded large icon ──────────────────────────────────────────
        Bitmap largeIcon = buildAlertIcon(currency, condition);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CH_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(flag + " " + fmt.format(current) + " TZS")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body)
                        .setBigContentTitle(title)
                        .setSummaryText("TanzRate  ·  " + currency + " / TZS"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(tapPi)
                .setAutoCancel(true)
                .setColor(Color.parseColor(condition == 0 ? "#4CAF50" : "#F44336"))
                .setColorized(true)
                .addAction(android.R.drawable.ic_menu_view, "View App", tapPi)
                .addAction(android.R.drawable.ic_delete, "Dismiss", dismissPi)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE);

        // Progress bar: how far current is from target (capped at 100%)
        int progress = (int) Math.min(100,
                condition == 0 ? (current / target * 100) : (target / current * 100));
        nb.setProgress(100, progress, false);

        try {
            NotificationManagerCompat.from(ctx).notify(alertIdCounter++, nb.build());
        } catch (SecurityException ignored) { /* Permission not granted */ }
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Post a low-priority rate-update summary notification.
     * Uses a fixed ID so it replaces the previous update notification.
     */
    public static void postRateUpdate(Context ctx, double usd, double eur, double gbp) {
        DecimalFormat fmt = new DecimalFormat("#,##0");
        String title = "🇹🇿  TanzRate — Rates Updated";
        String body  = "🇺🇸 USD  " + fmt.format(usd) + "   "
                + "🇪🇺 EUR  " + fmt.format(eur) + "   "
                + "🇬🇧 GBP  " + fmt.format(gbp);
        String expanded = "USD = " + new DecimalFormat("#,##0.00").format(usd) + " TZS\n"
                        + "EUR = " + new DecimalFormat("#,##0.00").format(eur) + " TZS\n"
                        + "GBP = " + new DecimalFormat("#,##0.00").format(gbp) + " TZS";

        Intent openApp = new Intent(ctx, TanzaniaForexApp.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0,
                openApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CH_UPDATES)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setLargeIcon(buildUpdateIcon())
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(expanded)
                        .setBigContentTitle(title))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#FFD700"));

        try {
            NotificationManagerCompat.from(ctx).notify(ID_UPDATE_BASE, nb.build());
        } catch (SecurityException ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── Icon builders ─────────────────────────────────────────────────────────

    /** Draws a coloured circle with an up/down arrow for the alert large icon. */
    private static Bitmap buildAlertIcon(String currency, int condition) {
        int size = 128;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(condition == 0 ? Color.parseColor("#1B3A1F") : Color.parseColor("#3A1B1B"));
        c.drawOval(new RectF(0, 0, size, size), bg);

        // Ring border
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(6f);
        ring.setColor(condition == 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        c.drawOval(new RectF(3, 3, size - 3, size - 3), ring);

        // Arrow text
        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        txt.setColor(Color.WHITE);
        txt.setTextSize(52f);
        txt.setTypeface(Typeface.DEFAULT_BOLD);
        txt.setTextAlign(Paint.Align.CENTER);
        c.drawText(condition == 0 ? "▲" : "▼", size / 2f, size / 2f + 18f, txt);

        // Currency code
        Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
        sub.setColor(Color.parseColor("#CCCCCC"));
        sub.setTextSize(22f);
        sub.setTextAlign(Paint.Align.CENTER);
        sub.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText(currency.length() > 3 ? "FX" : currency, size / 2f, size - 10f, sub);

        return bmp;
    }

    /** Gold TZ flag icon for update notifications. */
    private static Bitmap buildUpdateIcon() {
        int size = 128;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.parseColor("#1A1A2E"));
        c.drawOval(new RectF(0, 0, size, size), bg);

        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(5f);
        ring.setColor(Color.parseColor("#FFD700"));
        c.drawOval(new RectF(3, 3, size - 3, size - 3), ring);

        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        txt.setColor(Color.parseColor("#FFD700"));
        txt.setTextSize(44f);
        txt.setTextAlign(Paint.Align.CENTER);
        c.drawText("TZ", size / 2f, size / 2f + 14f, txt);

        return bmp;
    }
}
