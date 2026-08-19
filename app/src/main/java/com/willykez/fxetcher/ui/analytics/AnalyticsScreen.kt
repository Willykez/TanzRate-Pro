package com.willykez.fxetcher.ui.analytics

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.willykez.fxetcher.data.RatePoint
import com.willykez.fxetcher.ui.FxViewModel
import com.willykez.fxetcher.ui.components.AreaSparkline
import com.willykez.fxetcher.ui.components.SectionCard
import com.willykez.fxetcher.ui.components.SectionHeader
import com.willykez.fxetcher.ui.strings.LocalStrings
import com.willykez.fxetcher.ui.strings.Strings
import com.willykez.fxetcher.ui.theme.Blue
import com.willykez.fxetcher.ui.theme.Gold
import com.willykez.fxetcher.ui.theme.Green
import com.willykez.fxetcher.ui.theme.Purple
import com.willykez.fxetcher.ui.theme.Red
import java.text.DecimalFormat

@Composable
fun AnalyticsScreen(vm: FxViewModel) {
    val strings = LocalStrings.current
    val history by vm.rateHistory.collectAsState()

    var selected by remember { mutableStateOf("USD") }
    var compareWith by remember { mutableStateOf<String?>(null) }
    val trackedCodes = remember { (CurrencyMeta.LIVE_TOP + CurrencyMeta.EAST_AFRICA + listOf("XAU", "XAG")).distinct() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionCard {
                SectionHeader("📊", strings.analyticsTitle, strings.analyticsSubtitle, Purple)
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trackedCodes) { code ->
                        val currency = CurrencyMeta.of(code)
                        FilterChip(
                            selected = selected == code,
                            onClick = { selected = code; if (compareWith == code) compareWith = null },
                            label = { Text("${currency.flag} $code") }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(strings.compareWith, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = compareWith == null,
                            onClick = { compareWith = null },
                            label = { Text(strings.compareNone) }
                        )
                    }
                    items(trackedCodes.filter { it != selected }) { code ->
                        val currency = CurrencyMeta.of(code)
                        FilterChip(
                            selected = compareWith == code,
                            onClick = { compareWith = code },
                            label = { Text("${currency.flag} $code") }
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))

                val points = history[selected].orEmpty()
                if (points.size < 2) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                        Text(strings.notEnoughData, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val values = points.map { it.value }
                    val first = values.first()
                    val last = values.last()
                    val changePct = if (first != 0.0) (last - first) / first * 100.0 else 0.0
                    val up = changePct >= 0
                    val color = if (up) Green else Red

                    Text(
                        "${DecimalFormat("#,##0.00").format(last)} TZS",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${if (up) "▲" else "▼"} ${DecimalFormat("#,##0.00").format(kotlin.math.abs(changePct))}% ${strings.statChange.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                    Spacer(Modifier.height(16.dp))

                    val comparePoints = compareWith?.let { history[it]?.map { p -> p.value } }
                    if (compareWith != null && comparePoints != null && comparePoints.size >= 2) {
                        val compareCurrency = CurrencyMeta.of(compareWith!!)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LegendDot(color, "$selected")
                            Spacer(Modifier.width(14.dp))
                            LegendDot(Purple, "${compareCurrency.flag} $compareWith")
                        }
                        Spacer(Modifier.height(10.dp))
                        com.willykez.fxetcher.ui.components.DualTrendChart(
                            pointsA = values, pointsB = comparePoints,
                            colorA = color, colorB = Purple,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                        Text(
                            "Shown as % change from each currency's first tracked point",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        AreaSparkline(points = values, color = color, modifier = Modifier.fillMaxWidth().height(140.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    val high = values.max()
                    val low = values.min()
                    val avg = values.average()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatBlock(strings.statHigh, DecimalFormat("#,##0.0").format(high), Green)
                        StatBlock(strings.statLow, DecimalFormat("#,##0.0").format(low), Red)
                        StatBlock(strings.statAvg, DecimalFormat("#,##0.0").format(avg), Blue)
                        StatBlock(strings.statChange, "${if (up) "+" else ""}${DecimalFormat("#,##0.0").format(changePct)}%", color)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Based on ${points.size} tracked points this session",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            TopMoversCard(strings = strings, history = history, trackedCodes = trackedCodes)
        }

        item {
            PortfolioCard(vm = vm)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .size(10.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TopMoversCard(
    strings: Strings,
    history: Map<String, List<RatePoint>>,
    trackedCodes: List<String>
) {
    SectionCard {
        SectionHeader("🔥", strings.topMoversTitle, strings.topMoversSubtitle, Gold)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        val movers = trackedCodes.mapNotNull { code ->
            val pts = history[code]
            if (pts == null || pts.size < 2) return@mapNotNull null
            val first = pts.first().value
            val last = pts.last().value
            if (first == 0.0) return@mapNotNull null
            code to (last - first) / first * 100.0
        }.sortedByDescending { kotlin.math.abs(it.second) }.take(6)

        if (movers.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text(strings.notEnoughData, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                movers.forEach { (code, pct) ->
                    val up = pct >= 0
                    val color = if (up) Green else Red
                    val currency = CurrencyMeta.of(code)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${currency.flag}  ${currency.name}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${if (up) "▲" else "▼"} ${DecimalFormat("#,##0.00").format(kotlin.math.abs(pct))}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioCard(vm: FxViewModel) {
    val strings = LocalStrings.current
    val holdings by vm.portfolio.collectAsState()
    val rates by vm.rates.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    SectionCard {
        SectionHeader("💼", strings.portfolioTitle, strings.portfolioSubtitle, Blue)
        Spacer(Modifier.height(14.dp))

        if (holdings.isNotEmpty()) {
            val total = holdings.sumOf { h -> (rates[h.currency] ?: 0.0) * h.amount }
            Text(strings.portfolioTotal, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${DecimalFormat("#,##0.00").format(total)} TZS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Blue
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                holdings.forEachIndexed { i, h ->
                    val currency = CurrencyMeta.of(h.currency)
                    val currentRate = rates[h.currency]
                    val value = currentRate?.let { it * h.amount }
                    val gainPct = if (currentRate != null && h.rateAtAdd != 0.0) (currentRate - h.rateAtAdd) / h.rateAtAdd * 100.0 else null
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currency.flag, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("${DecimalFormat("#,##0.####").format(h.amount)} ${h.currency}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (gainPct != null) {
                                    val up = gainPct >= 0
                                    Text(
                                        "${if (up) "▲" else "▼"} ${DecimalFormat("#,##0.00").format(kotlin.math.abs(gainPct))}% ${strings.portfolioGain}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (up) Green else Red
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                value?.let { "${DecimalFormat("#,##0.00").format(it)} TZS" } ?: "—",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            androidx.compose.material3.TextButton(
                                onClick = { vm.deleteHolding(i) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text(strings.delete, style = MaterialTheme.typography.labelSmall, color = Red)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                Text(strings.portfolioEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        androidx.compose.material3.OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Text("➕ ${strings.addHolding}")
        }
    }

    if (showAdd) {
        AddHoldingSheet(
            rates = rates,
            onDismiss = { showAdd = false },
            onAdd = { code, amount -> vm.addHolding(code, amount); showAdd = false }
        )
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun AddHoldingSheet(
    rates: Map<String, Double>,
    onDismiss: () -> Unit,
    onAdd: (String, Double) -> Unit
) {
    val strings = LocalStrings.current
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    var code by remember { mutableStateOf("USD") }
    var amountText by remember { mutableStateOf("") }

    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(strings.addHolding, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            com.willykez.fxetcher.ui.components.CurrencyPickerField(strings.searchHint, code) { code = it }
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(strings.holdingAmount) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            rates[code]?.let {
                Spacer(Modifier.height(4.dp))
                Text("1 $code = ${DecimalFormat("#,##0.00").format(it)} TZS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) onAdd(code, amt)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(strings.addHolding) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
