package com.willykez.fxetcher.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.willykez.fxetcher.data.CurrencyMeta
import com.willykez.fxetcher.ui.FxViewModel
import com.willykez.fxetcher.ui.components.InfoRow
import com.willykez.fxetcher.ui.components.QuickConvertSheet
import com.willykez.fxetcher.ui.components.RateRow
import com.willykez.fxetcher.ui.components.SectionCard
import com.willykez.fxetcher.ui.components.SectionHeader
import com.willykez.fxetcher.ui.components.accentFor
import com.willykez.fxetcher.ui.strings.LocalStrings
import com.willykez.fxetcher.ui.theme.Blue
import com.willykez.fxetcher.ui.theme.Gold
import com.willykez.fxetcher.ui.theme.Orange
import com.willykez.fxetcher.ui.theme.Purple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: FxViewModel) {
    val rates by vm.rates.collectAsState()
    val prevRates by vm.prevRates.collectAsState()
    val rateHistory by vm.rateHistory.collectAsState()
    val fetching by vm.fetching.collectAsState()
    val botFetching by vm.botFetching.collectAsState()
    val botRates by vm.botRates.collectAsState()
    val botStatus by vm.botStatus.collectAsState()
    val watchlist by vm.watchlist.collectAsState()
    val context = LocalContext.current

    var quickConvertCode by remember { mutableStateOf<String?>(null) }

    PullToRefreshBox(
        isRefreshing = fetching || botFetching,
        onRefresh = { vm.refreshAll() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LiveRatesCard(
                    rates = rates, prevRates = prevRates, history = rateHistory,
                    watchlist = watchlist, fmt = vm.fmtTzs::format,
                    onRowClick = { quickConvertCode = it },
                    onFavorite = { vm.toggleWatchlist(it) },
                    onShare = {
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, vm.shareRatesText())
                        }
                        context.startActivity(Intent.createChooser(i, "Share Rates"))
                    }
                )
            }
            item {
                EastAfricaCard(
                    rates = rates, prevRates = prevRates, fmt = vm.fmtTzs::format, fmtSmall = vm.fmtSmall::format,
                    watchlist = watchlist,
                    onRowClick = { quickConvertCode = it },
                    onFavorite = { vm.toggleWatchlist(it) }
                )
            }
            item {
                MetalsCard(rates = rates, fmt = vm.fmtTzs::format, onRowClick = { quickConvertCode = it })
            }
            item {
                BotRatesCard(botRates = botRates, status = botStatus, fmt = vm.fmtTzs::format)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    quickConvertCode?.let { code ->
        QuickConvertSheet(
            code = code,
            tzsRate = rates[code],
            fmt = vm.fmtTzs::format,
            onDismiss = { quickConvertCode = null },
            onSave = { amount, result -> vm.saveConversion(amount, code, "TZS", result) }
        )
    }
}

@Composable
private fun LiveRatesCard(
    rates: Map<String, Double>,
    prevRates: Map<String, Double>,
    history: Map<String, List<com.willykez.fxetcher.data.RatePoint>>,
    watchlist: List<String>,
    fmt: (Double) -> String,
    onRowClick: (String) -> Unit,
    onFavorite: (String) -> Unit,
    onShare: () -> Unit
) {
    SectionCard {
        val strings = LocalStrings.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                SectionHeader("📈", strings.liveRatesTitle, strings.liveRatesSubtitle, Blue)
            }
            OutlinedButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.share)
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyMeta.LIVE_TOP.forEachIndexed { i, code ->
                val r = rates[code]
                val ov = prevRates[code]
                val pct = if (ov != null && ov > 0 && r != null) (r - ov) / ov * 100.0 else null
                val currency = CurrencyMeta.of(code)
                RateRow(
                    flag = currency.flag,
                    name = currency.name,
                    code = code,
                    valueText = r?.let { "${fmt(it)} TZS" } ?: "Loading…",
                    changePct = pct,
                    accent = accentFor(i),
                    isFavorite = watchlist.contains(code),
                    sparkline = history[code]?.map { it.value } ?: emptyList(),
                    onClick = { onRowClick(code) },
                    onLongClick = { onFavorite(code) },
                    onFavoriteClick = { onFavorite(code) }
                )
            }
        }
    }
}

@Composable
private fun EastAfricaCard(
    rates: Map<String, Double>,
    prevRates: Map<String, Double>,
    fmt: (Double) -> String,
    fmtSmall: (Double) -> String,
    watchlist: List<String>,
    onRowClick: (String) -> Unit,
    onFavorite: (String) -> Unit
) {
    SectionCard {
        val strings = LocalStrings.current
        SectionHeader("🌍", strings.eastAfricaTitle, strings.eastAfricaSubtitle, Orange)
        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyMeta.EAST_AFRICA.forEachIndexed { i, code ->
                val r = rates[code]
                val ov = prevRates[code]
                val pct = if (ov != null && ov > 0 && r != null) (r - ov) / ov * 100.0 else null
                val currency = CurrencyMeta.of(code)
                val small = CurrencyMeta.isSmallRate(code)
                RateRow(
                    flag = currency.flag,
                    name = currency.name,
                    code = code,
                    valueText = r?.let { "${if (small) fmtSmall(it) else fmt(it)} TZS" } ?: "—",
                    changePct = pct,
                    accent = accentFor(i + 2),
                    isFavorite = watchlist.contains(code),
                    onClick = { onRowClick(code) },
                    onLongClick = { onFavorite(code) },
                    onFavoriteClick = { onFavorite(code) }
                )
            }
        }
    }
}

@Composable
private fun MetalsCard(rates: Map<String, Double>, fmt: (Double) -> String, onRowClick: (String) -> Unit) {
    SectionCard {
        val strings = LocalStrings.current
        SectionHeader("⚖️", strings.metalsTitle, strings.metalsSubtitle, Gold)
        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetalTile("🥇", "Gold", "XAU", rates["XAU"], fmt, Gold, Modifier.weight(1f)) { onRowClick("XAU") }
            MetalTile("🥈", "Silver", "XAG", rates["XAG"], fmt, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f)) { onRowClick("XAG") }
        }
    }
}

@Composable
private fun MetalTile(
    icon: String, label: String, code: String, value: Double?,
    fmt: (Double) -> String, accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.Bold)
            Text(LocalStrings.current.perTroyOz, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                value?.let { "${fmt(it)}" } ?: "Loading…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("TZS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BotRatesCard(
    botRates: List<com.willykez.fxetcher.data.BotRate>,
    status: String,
    fmt: (Double) -> String
) {
    SectionCard {
        SectionHeader("🏦", "BoT Official Rates", "Bank of Tanzania · bot.go.tz", Purple)
        Spacer(Modifier.height(10.dp))
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        if (botRates.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No data — pull down to fetch", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@SectionCard
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("Currency", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Gold, fontWeight = FontWeight.Bold)
            Text("Buying", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Gold, fontWeight = FontWeight.Bold)
            Text("Selling", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Gold, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()
        botRates.filter { it.currency.isNotEmpty() && it.currency.length <= 4 }.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    "${CurrencyMeta.of(r.currency).flag}  ${r.currency}",
                    Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium
                )
                Text(r.buying, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = com.willykez.fxetcher.ui.theme.Green)
                Text(r.selling, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = com.willykez.fxetcher.ui.theme.Red)
            }
        }
    }
}
