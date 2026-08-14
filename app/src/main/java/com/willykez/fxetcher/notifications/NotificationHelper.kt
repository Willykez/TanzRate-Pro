package com.willykez.fxetcher.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.willykez.fxetcher.MainActivity
import java.text.DecimalFormat

object NotificationHelper {

    const val CH_UPDATES = "rate_updates"
    const val CH_ALERTS = "price_alerts"
    const val ID_RATE_UPDATE = 1001

    private val fmt = DecimalFormat("#,##0.00")

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CH_UPDATES, "Rate Updates", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_ALERTS, "Price Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    fun postRateUpdate(ctx: Context, usd: Double?, eur: Double?, gbp: Double?) {
        val body = buildString {
            usd?.let { append("USD: ${fmt.format(it)}\n") }
            eur?.let { append("EUR: ${fmt.format(it)}\n") }
            gbp?.let { append("GBP: ${fmt.format(it)}") }
        }.trim()
        if (body.isEmpty()) return

        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nb = NotificationCompat.Builder(ctx, CH_UPDATES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(dotIcon(0xFF9C27B0.toInt()))
            .setContentTitle("FX Rate Update")
            .setContentText("Tap to view latest rates")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        if (hasPermission(ctx)) NotificationManagerCompat.from(ctx).notify(ID_RATE_UPDATE, nb)
    }

    fun postPriceAlert(ctx: Context, currency: String, condition: Int, target: Double, current: Double) {
        val isAbove = current > target
        val color = if (isAbove) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
        val body = (if (isAbove) "Above target: " else "Below target: ") +
            "${fmt.format(current)} (Target: ${fmt.format(target)})"

        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nb = NotificationCompat.Builder(ctx, CH_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(dotIcon(color))
            .setContentTitle("$currency Alert")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setColor(color)
            .setColorized(true)
            .build()

        if (hasPermission(ctx)) NotificationManagerCompat.from(ctx).notify(currency.hashCode(), nb)
    }

    private fun hasPermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun dotIcon(color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(100f, 100f, 90f, paint)
        return bmp
    }
}
