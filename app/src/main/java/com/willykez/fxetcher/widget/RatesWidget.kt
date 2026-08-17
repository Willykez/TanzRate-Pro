package com.willykez.fxetcher.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.willykez.fxetcher.MainActivity
import com.willykez.fxetcher.data.CurrencyMeta
import com.willykez.fxetcher.data.RatesRepository
import com.willykez.fxetcher.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WidgetBg = ColorProvider(Color(0xFF12162B))
private val WidgetGold = ColorProvider(Color(0xFFFFC94A))
private val WidgetWhite = ColorProvider(Color(0xFFF2F3F8))
private val WidgetMuted = ColorProvider(Color(0xFFAEB4CF))

/** Home-screen widget showing USD/EUR/GBP/Gold vs TZS, refreshed by [RatesWidgetWorker]. */
class RatesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = UserPreferencesRepository(context)
        val rates = prefs.ratesFlow.first().ifEmpty { RatesRepository.fallbackRates() }
        val lastUpdate = prefs.lastUpdateFlow.first()
        val fmt = DecimalFormat("#,##0.00")
        val featured = prefs.widgetCurrenciesFlow.first().ifEmpty { UserPreferencesRepository.DEFAULT_WIDGET_CURRENCIES }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetBg)
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🇹🇿", style = TextStyle(fontSize = 16.sp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        "FXetcher",
                        style = TextStyle(color = WidgetWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                }
                Spacer(GlanceModifier.height(10.dp))
                featured.forEach { code ->
                    val currency = CurrencyMeta.of(code)
                    val value = rates[code]
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${currency.flag} $code",
                            style = TextStyle(color = WidgetMuted, fontSize = 13.sp),
                            modifier = GlanceModifier.width(64.dp)
                        )
                        Text(
                            value?.let { "${fmt.format(it)} TZS" } ?: "—",
                            style = TextStyle(color = WidgetGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        )
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    if (lastUpdate > 0) "Updated ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastUpdate))}" else "Tap to open",
                    style = TextStyle(color = WidgetMuted, fontSize = 10.sp)
                )
            }
        }
    }
}

/**
 * Re-renders every placed instance of the widget. Uses [GlanceAppWidgetManager]
 * plus the per-instance [GlanceAppWidget.update] — the foundational, stable
 * primitive that Glance's own convenience wrappers are built on top of.
 */
suspend fun refreshRatesWidget(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(RatesWidget::class.java)
    val widget = RatesWidget()
    glanceIds.forEach { id -> widget.update(context, id) }
}
