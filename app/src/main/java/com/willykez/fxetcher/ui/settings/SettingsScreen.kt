package com.willykez.fxetcher.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.willykez.fxetcher.data.ThemeMode
import com.willykez.fxetcher.ui.FxViewModel
import com.willykez.fxetcher.ui.components.InfoRow
import com.willykez.fxetcher.ui.components.SectionCard
import com.willykez.fxetcher.ui.components.SectionHeader
import com.willykez.fxetcher.ui.theme.Blue
import com.willykez.fxetcher.ui.theme.Gold
import com.willykez.fxetcher.ui.theme.Orange
import com.willykez.fxetcher.ui.theme.Purple
import com.willykez.fxetcher.ui.theme.Red
import com.willykez.fxetcher.ui.theme.Teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: FxViewModel) {
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    var confirmDialog by remember { mutableStateOf<ConfirmAction?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionCard {
                SectionHeader("🔄", "Auto-Refresh", "Keep rates up to date automatically", Blue)
                Spacer(Modifier.height(14.dp))
                ToggleRow("Auto-Refresh Rates", "Fetch latest rates in the background", settings.autoRefresh) {
                    vm.setAutoRefresh(it)
                }
                Spacer(Modifier.height(10.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))
                Text("Refresh Interval", style = MaterialTheme.typography.titleSmall)
                Text("How often to auto-refresh rates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                val intervals = listOf(30_000 to "30s", 60_000 to "1m", 300_000 to "5m", 600_000 to "10m", 900_000 to "15m", 1_800_000 to "30m")
                IntervalGrid(intervals, settings.refreshIntervalMs) { vm.setRefreshInterval(it) }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { vm.refreshAll() }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔄 Refresh Now")
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("🎨", "Appearance", "Choose your preferred theme", Purple)
                Spacer(Modifier.height(14.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size)
                        ) {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "🌗 System"
                                    ThemeMode.LIGHT -> "☀️ Light"
                                    ThemeMode.DARK -> "🌙 Dark"
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ToggleRow("Dynamic Color (Material You)", "Use wallpaper-based colors on Android 12+", settings.dynamicColor) {
                    vm.setDynamicColor(it)
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("📐", "Display Options", "Customize how rates are shown", Teal)
                Spacer(Modifier.height(14.dp))
                ToggleRow("Compact Mode", "Show more rates with less spacing", settings.compactMode) {
                    vm.setCompactMode(it)
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("🔔", "Notifications", "Rate alerts and refresh notices", Orange)
                Spacer(Modifier.height(14.dp))
                ToggleRow("Rate Update Notifications", "Show a notification on each refresh", settings.notifyUpdates) {
                    vm.setNotifyUpdates(it)
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("💾", "Data Management", "Clear stored data", Purple)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = {
                    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, vm.shareRatesText()) }
                    context.startActivity(Intent.createChooser(i, "Share Rates"))
                }, modifier = Modifier.fillMaxWidth()) { Text("📤 Export Rates as Text") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmDialog = ConfirmAction.ClearHistory }, modifier = Modifier.fillMaxWidth()) {
                    Text("🗑 Clear Conversion History", color = Red)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmDialog = ConfirmAction.ClearWatchlist }, modifier = Modifier.fillMaxWidth()) {
                    Text("⭐ Clear Watchlist", color = Orange)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmDialog = ConfirmAction.ClearAlerts }, modifier = Modifier.fillMaxWidth()) {
                    Text("🗑 Clear All Alerts", color = Red)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmDialog = ConfirmAction.ResetAll }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔁 Reset All App Data", color = Red)
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("⚖️", "Metals Measurement", "Troy Ounce conversions", Gold)
                Spacer(Modifier.height(14.dp))
                Text(
                    "Precious metals (Gold & Silver) are universally priced in Troy Ounces (oz t), which are heavier than standard avoirdupois ounces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(6.dp))
                InfoRow("1 Troy Ounce", "= 31.1035 Grams")
                InfoRow("1 Kilogram", "= 32.1507 Troy Oz")
                InfoRow("1 Standard Oz", "= 28.3495 Grams")
                InfoRow("Gold Purity 24K", "= 99.9% pure")
                InfoRow("Gold Purity 18K", "= 75% pure")
            }
        }

        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🇹🇿", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("FXetcher", style = MaterialTheme.typography.headlineMedium, color = Gold)
                        Text("Tanzania Forex Tracker", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                InfoRow("Version", "5.0.0")
                InfoRow("Package", "com.willykez.fxetcher")
                InfoRow("Currencies", "25 currencies + 2 metals")
                InfoRow("Forex Data", "ExchangeRate-API v6")
                InfoRow("Metals Data", "MetalPriceAPI")
                InfoRow("BoT Data", "bot.go.tz (scraped)")
                InfoRow("Base Currency", "Tanzanian Shilling (TZS)")
                Spacer(Modifier.height(10.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ Exchange rates are for informational purposes only. Always verify with your bank before financial decisions. BoT rates are official but may have a brief publication delay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "🇹🇿 FXetcher — Tanzania Forex Tracker\nLive exchange rates for Tanzanian Shilling.")
                    }
                    context.startActivity(Intent.createChooser(i, "Share FXetcher"))
                }, modifier = Modifier.fillMaxWidth()) { Text("📤 Share FXetcher") }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    confirmDialog?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmDialog = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        ConfirmAction.ClearHistory -> vm.clearConversions()
                        ConfirmAction.ClearWatchlist -> vm.clearWatchlist()
                        ConfirmAction.ClearAlerts -> vm.clearAlerts()
                        ConfirmAction.ResetAll -> vm.resetAllData()
                    }
                    confirmDialog = null
                }) { Text(action.confirmLabel, color = Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDialog = null }) { Text("Cancel") } }
        )
    }
}

private enum class ConfirmAction(val title: String, val message: String, val confirmLabel: String) {
    ClearHistory("Clear History", "Delete all saved conversions?", "Clear"),
    ClearWatchlist("Clear Watchlist", "Remove all watchlist currencies?", "Clear"),
    ClearAlerts("Clear Alerts", "Delete all price alerts?", "Clear"),
    ResetAll("Reset App", "Clear all stored rates, history, alerts, and settings?", "Reset")
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IntervalGrid(intervals: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        intervals.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (ms, label) ->
                    val isSel = ms == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Blue.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelect(ms) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (isSel) Blue else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
