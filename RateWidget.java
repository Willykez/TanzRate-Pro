package com.willykez.tanzs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.widget.RemoteViews;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TanzRate home-screen widget.
 *
 * Displays the 3 most-used rates (USD, EUR, GBP → TZS) on a dark branded card.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * AndroidManifest.xml — add inside <application>:
 *
 *   <receiver android:name=".RateWidget" android:exported="true">
 *       <intent-filter>
 *           <action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
 *       </intent-filter>
 *       <meta-data
 *           android:name="android.appwidget.provider"
 *           android:resource="@xml/rate_widget_info"/>
 *   </receiver>
 *
 * res/xml/rate_widget_info.xml:
 *   <?xml version="1.0" encoding="utf-8"?>
 *   <appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
 *       android:minWidth="250dp"
 *       android:minHeight="110dp"
 *       android:updatePeriodMillis="1800000"
 *       android:initialLayout="@layout/rate_widget_layout"
 *       android:resizeMode="horizontal|vertical"
 *       android:widgetCategory="home_screen"/>
 *
 * res/layout/rate_widget_layout.xml  — see rate_widget_layout.xml in this output.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class RateWidget extends AppWidgetProvider {

    static final String PREFS     = "TanzSPrefs";
    static final String KEY_RATES = "rates_v2";

    // ── Called by Android on every update interval ────────────────────────────
    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    // ── Public static so the main app can also trigger a widget refresh ────────
    public static void refreshAll(Context ctx) {
        AppWidgetManager mgr   = AppWidgetManager.getInstance(ctx);
        ComponentName  comp    = new ComponentName(ctx, RateWidget.class);
        int[]          ids     = mgr.getAppWidgetIds(comp);
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        // ── Load saved rates ──────────────────────────────────────────────────
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        double usd = 2_600, eur = 2_820, gbp = 3_310, kes = 20;
        try {
            org.json.JSONObject j = new org.json.JSONObject(prefs.getString(KEY_RATES, "{}"));
            if (j.has("USD")) usd = j.getDouble("USD");
            if (j.has("EUR")) eur = j.getDouble("EUR");
            if (j.has("GBP")) gbp = j.getDouble("GBP");
            if (j.has("KES")) kes = j.getDouble("KES");
        } catch (Exception ignored) {}

        // ── Draw custom Bitmap widget ─────────────────────────────────────────
        int W = 600, H = 220;
        Bitmap bmp = drawWidgetBitmap(ctx, W, H, usd, eur, gbp, kes);

        // ── Tap → open TanzaniaForexApp ───────────────────────────────────────
        Intent launch = new Intent(ctx, TanzaniaForexApp.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ── Build RemoteViews using android.R.layout.simple_list_item_1 as
        //    a container (we replace the content with our bitmap via ImageView)
        RemoteViews rv = new RemoteViews(ctx.getPackageName(),
                R.layout.rate_widget_layout);
        rv.setImageViewBitmap(R.id.widget_image, bmp);
        rv.setOnClickPendingIntent(R.id.widget_image, pi);

        mgr.updateAppWidget(widgetId, rv);
    }

    // ─────────────────────────────────────────────────────────────────────────
    /** Draw the entire widget as a Bitmap so we control every pixel. */
    private static Bitmap drawWidgetBitmap(Context ctx, int W, int H,
                                            double usd, double eur,
                                            double gbp, double kes) {
        DecimalFormat fmt = new DecimalFormat("#,##0");
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);
        float  r   = 32f;

        // ── Background gradient ───────────────────────────────────────────────
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient grad = new LinearGradient(0, 0, W, H,
                Color.parseColor("#0D1230"), Color.parseColor("#1A2045"),
                Shader.TileMode.CLAMP);
        bgPaint.setShader(grad);
        c.drawRoundRect(new RectF(0, 0, W, H), r, r, bgPaint);

        // ── Gold border ───────────────────────────────────────────────────────
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(3f);
        border.setColor(Color.parseColor("#FFD70066"));
        c.drawRoundRect(new RectF(1.5f, 1.5f, W - 1.5f, H - 1.5f), r, r, border);

        // ── App badge (top-left) ──────────────────────────────────────────────
        Paint badge = new Paint(Paint.ANTI_ALIAS_FLAG);
        badge.setColor(Color.parseColor("#FFD70022"));
        c.drawRoundRect(new RectF(16, 12, 140, 42), 14, 14, badge);

        Paint badgeTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeTxt.setColor(Color.parseColor("#FFD700"));
        badgeTxt.setTextSize(22f);
        badgeTxt.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("🇹🇿  TanzRate", 24, 33, badgeTxt);

        // ── Last updated (top-right) ──────────────────────────────────────────
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(Color.parseColor("#888888"));
        timePaint.setTextSize(20f);
        timePaint.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Updated " + timeStr, W - 16, 33, timePaint);

        // ── Divider ───────────────────────────────────────────────────────────
        Paint div = new Paint();
        div.setColor(Color.parseColor("#2A2F4A"));
        div.setStrokeWidth(1f);
        c.drawLine(16, 50, W - 16, 50, div);

        // ── Rate rows ─────────────────────────────────────────────────────────
        String[][] rows = {
            {"🇺🇸", "USD", fmt.format(usd)},
            {"🇪🇺", "EUR", fmt.format(eur)},
            {"🇬🇧", "GBP", fmt.format(gbp)},
            {"🇰🇪", "KES", fmt.format(kes)},
        };

        int rowH  = (H - 60) / rows.length;
        String[] rowColors = {"#4CAF50","#FFD700","#2196F3","#FF9800"};

        for (int i = 0; i < rows.length; i++) {
            float y = 60 + i * rowH;

            // Subtle alternating background
            if (i % 2 == 0) {
                Paint rowBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                rowBg.setColor(Color.parseColor("#FFFFFF08"));
                c.drawRoundRect(new RectF(12, y + 2, W - 12, y + rowH - 2), 8, 8, rowBg);
            }

            // Left colour accent bar
            Paint accentBar = new Paint(Paint.ANTI_ALIAS_FLAG);
            accentBar.setColor(Color.parseColor(rowColors[i]));
            accentBar.setAlpha(180);
            c.drawRoundRect(new RectF(14, y + 6, 20, y + rowH - 6), 3, 3, accentBar);

            // Flag
            Paint flagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            flagPaint.setTextSize(28f);
            c.drawText(rows[i][0], 28, y + rowH / 2f + 10f, flagPaint);

            // Currency code
            Paint codePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            codePaint.setColor(Color.parseColor("#CCCCCC"));
            codePaint.setTextSize(24f);
            codePaint.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText(rows[i][1], 72, y + rowH / 2f + 8f, codePaint);

            // "= X TZS" right-aligned
            Paint valPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            valPaint.setColor(Color.parseColor(rowColors[i]));
            valPaint.setTextSize(26f);
            valPaint.setTypeface(Typeface.DEFAULT_BOLD);
            valPaint.setTextAlign(Paint.Align.RIGHT);
            c.drawText(rows[i][2] + " TZS", W - 20, y + rowH / 2f + 9f, valPaint);
        }

        return bmp;
    }
}
