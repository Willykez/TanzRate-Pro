package com.willykez.fxetcher.ui.markets

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.willykez.fxetcher.data.CurrencyMeta
import com.willykez.fxetcher.data.PriceAlert
import com.willykez.fxetcher.ui.FxViewModel
import com.willykez.fxetcher.ui.components.CurrencyPickerField
import com.willykez.fxetcher.ui.components.FieldLabel
import com.willykez.fxetcher.ui.components.QuickConvertSheet
import com.willykez.fxetcher.ui.components.RateRow
import com.willykez.fxetcher.ui.components.SectionCard
import com.willykez.fxetcher.ui.components.SectionHeader
import com.willykez.fxetcher.ui.components.accentFor
import com.willykez.fxetcher.ui.strings.LocalStrings
import com.willykez.fxetcher.ui.theme.Blue
import com.willykez.fxetcher.ui.theme.Gold
import com.willykez.fxetcher.ui.theme.Green
import com.willykez.fxetcher.ui.theme.Orange
import com.willykez.fxetcher.ui.theme.Purple
import com.willykez.fxetcher.ui.theme.Red
import com.willykez.fxetcher.ui.theme.Teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(vm: FxViewModel) {
    val strings = LocalStrings.current
    val rates by vm.rates.collectAsState()
    val prevRates by vm.prevRates.collectAsState()
    val history by vm.rateHistory.collectAsState()
    val watchlist by vm.watchlist.collectAsState()
    val alerts by vm.alerts.collectAsState()

    var quickConvertCode by remember { mutableStateOf<String?>(null) }
    var showAddAlert by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WatchlistCard(
                watchlist = watchlist, rates = rates, prevRates = prevRates, history = history,
                fmt = vm.fmtTzs::format,
                onRowClick = { quickConvertCode = it },
                onFavorite = { vm.toggleWatchlist(it) }
            )
        }
        item {
            RateSectionCard(
                icon = "💵", title = strings.majorTitle, subtitle = strings.majorSubtitle, accent = Blue,
                codes = CurrencyMeta.LIVE_TOP, rates = rates, prevRates = prevRates, history = history,
                watchlist = watchlist, fmt = vm.fmtTzs::format,
                onRowClick = { quickConvertCode = it }, onFavorite = { vm.toggleWatchlist(it) }
            )
        }
        item {
            RateSectionCard(
                icon = "🌍", title = strings.eastAfricaTitle, subtitle = strings.eastAfricaSubtitle, accent = Orange,
                codes = CurrencyMeta.EAST_AFRICA, rates = rates, prevRates = prevRates, history = history,
                watchlist = watchlist, fmt = vm.fmtTzs::format,
                onRowClick = { quickConvertCode = it }, onFavorite = { vm.toggleWatchlist(it) }
            )
        }
        item {
            SectionCard {
                SectionHeader("⚖️", strings.metalsTitle, "Extended metals reference", Gold)
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                listOf("XAU", "XAG").forEach { code ->
                    val r = rates[code]
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${CurrencyMeta.of(code).flag} ${CurrencyMeta.of(code).name}", style = MaterialTheme.typography.bodyMedium)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(r?.let { "${vm.fmtTzs.format(it)} TZS" } ?: strings.loading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(strings.perTroyOz, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            AlertsCard(alerts = alerts, rates = rates, fmt = vm.fmtTzs::format, onAdd = { showAddAlert = true }, onDelete = { vm.deleteAlert(it) })
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    quickConvertCode?.let { code ->
        QuickConvertSheet(
            code = code, tzsRate = rates[code], fmt = vm.fmtTzs::format,
            onDismiss = { quickConvertCode = null },
            onSave = { amount, result -> vm.saveConversion(amount, code, "TZS", result) }
        )
    }

    if (showAddAlert) {
        AddAlertSheet(
            rates = rates, fmt = vm.fmtTzs::format,
            onDismiss = { showAddAlert = false },
            onCreate = { code, target, cond -> vm.addAlert(code, target, cond) }
        )
    }
}

@Composable
private fun WatchlistCard(
    watchlist: List<String>,
    rates: Map<String, Double>,
    prevRates: Map<String, Double>,
    history: Map<String, List<com.willykez.fxetcher.data.RatePoint>>,
    fmt: (Double) -> String,
    onRowClick: (String) -> Unit,
    onFavorite: (String) -> Unit
) {
    SectionCard {
        val strings = LocalStrings.current
        SectionHeader("⭐", strings.watchlistTitle, strings.watchlistSubtitle, Gold)
        Spacer(Modifier.height(10.dp))
        if (watchlist.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                Text(
                    strings.watchlistEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                watchlist.forEachIndexed { i, code ->
                    val r = rates[code]
                    val ov = prevRates[code]
                    val pct = if (ov != null && ov > 0 && r != null) (r - ov) / ov * 100.0 else null
                    val currency = CurrencyMeta.of(code)
                    RateRow(
                        flag = currency.flag, name = currency.name, code = code,
                        valueText = r?.let { "${fmt(it)} TZS" } ?: "—", changePct = pct,
                        accent = accentFor(i), isFavorite = true,
                        sparkline = history[code]?.map { it.value } ?: emptyList(),
                        onClick = { onRowClick(code) }, onFavoriteClick = { onFavorite(code) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RateSectionCard(
    icon: String, title: String, subtitle: String, accent: androidx.compose.ui.graphics.Color,
    codes: List<String>, rates: Map<String, Double>, prevRates: Map<String, Double>,
    history: Map<String, List<com.willykez.fxetcher.data.RatePoint>>,
    watchlist: List<String>, fmt: (Double) -> String,
    onRowClick: (String) -> Unit, onFavorite: (String) -> Unit
) {
    SectionCard {
        SectionHeader(icon, title, subtitle, accent)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            codes.forEachIndexed { i, code ->
                val r = rates[code]
                val ov = prevRates[code]
                val pct = if (ov != null && ov > 0 && r != null) (r - ov) / ov * 100.0 else null
                val currency = CurrencyMeta.of(code)
                RateRow(
                    flag = currency.flag, name = currency.name, code = code,
                    valueText = r?.let { "${fmt(it)} TZS" } ?: "—", changePct = pct,
                    accent = accentFor(i), isFavorite = watchlist.contains(code),
                    sparkline = history[code]?.map { it.value } ?: emptyList(),
                    onClick = { onRowClick(code) }, onLongClick = { onFavorite(code) },
                    onFavoriteClick = { onFavorite(code) }
                )
            }
        }
    }
}

@Composable
private fun AlertsCard(
    alerts: List<PriceAlert>,
    rates: Map<String, Double>,
    fmt: (Double) -> String,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit
) {
    SectionCard {
        val strings = LocalStrings.current
        SectionHeader("🔔", strings.alertsTitle, strings.alertsSubtitle, Blue)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.width(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.addAlert)
        }
        Spacer(Modifier.height(12.dp))
        if (alerts.isEmpty()) {
            HorizontalDivider()
            Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                Text(strings.noAlerts, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alerts.forEachIndexed { i, alert ->
                    val up = alert.condition == 0
                    val cur = rates[alert.currency]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .width(4.dp).height(40.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (up) Green else Red)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            val strings = LocalStrings.current
                            Text(
                                "${CurrencyMeta.of(alert.currency).flag}  ${alert.currency}  ${if (up) "▲ ${strings.risesAbove.lowercase()}" else "▼ ${strings.fallsBelow.lowercase()}"}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text("${strings.target}: ${fmt(alert.target)} TZS", style = MaterialTheme.typography.labelSmall, color = Gold)
                            if (cur != null) {
                                val diff = cur - alert.target
                                Text(
                                    "${strings.current}: ${fmt(cur)} TZS  (${if (diff >= 0) "+" else ""}${fmt(diff)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cur >= alert.target) Green else Red
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(i) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Red)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlertSheet(
    rates: Map<String, Double>,
    fmt: (Double) -> String,
    onDismiss: () -> Unit,
    onCreate: (String, Double, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val strings = LocalStrings.current
    var code by remember { mutableStateOf("USD") }
    var condition by remember { mutableStateOf(0) }
    var targetText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Blue)
                Spacer(Modifier.width(8.dp))
                Text(strings.newAlertTitle, style = MaterialTheme.typography.titleLarge)
            }
            Text(strings.newAlertSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            FieldLabel("Currency")
            Spacer(Modifier.height(6.dp))
            CurrencyPickerField("Currency", code) { code = it }
            Spacer(Modifier.height(12.dp))
            FieldLabel("Condition")
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = condition == 0, onClick = { condition = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("📈 ${strings.risesAbove}") }
                SegmentedButton(
                    selected = condition == 1, onClick = { condition = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("📉 ${strings.fallsBelow}") }
            }
            Spacer(Modifier.height(12.dp))
            FieldLabel("Target Rate (TZS)")
            Spacer(Modifier.height(6.dp))
            val currentRate = rates[code]
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = { currentRate?.let { Text("e.g. ${fmt(it)}") } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            currentRate?.let {
                Spacer(Modifier.height(4.dp))
                Text("${strings.current}: ${fmt(it)} TZS", style = MaterialTheme.typography.labelSmall, color = Green)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull()
                    if (target != null && target > 0) {
                        onCreate(code, target, condition)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("🔔 ${strings.createAlert}") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
