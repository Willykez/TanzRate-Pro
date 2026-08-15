package com.willykez.fxetcher.ui.analytics

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
                            onClick = { selected = code },
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
                    AreaSparkline(points = values, color = color, modifier = Modifier.fillMaxWidth().height(140.dp))
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
