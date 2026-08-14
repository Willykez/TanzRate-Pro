package com.willykez.fxetcher.ui.convert

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.willykez.fxetcher.data.CurrencyMeta
import com.willykez.fxetcher.ui.FxViewModel
import com.willykez.fxetcher.ui.components.CurrencyPickerField
import com.willykez.fxetcher.ui.components.FieldLabel
import com.willykez.fxetcher.ui.components.SectionCard
import com.willykez.fxetcher.ui.components.SectionHeader
import com.willykez.fxetcher.ui.theme.Blue
import com.willykez.fxetcher.ui.theme.Gold
import com.willykez.fxetcher.ui.theme.Green
import com.willykez.fxetcher.ui.theme.Orange
import com.willykez.fxetcher.ui.theme.Teal
import java.text.DecimalFormat

@Composable
fun ConvertScreen(vm: FxViewModel) {
    val rates by vm.rates.collectAsState()
    val pinnedFrom by vm.pinnedFrom.collectAsState()
    val pinnedTo by vm.pinnedTo.collectAsState()
    val history by vm.convHistory.collectAsState()
    val context = LocalContext.current

    var amountText by remember { mutableStateOf("1") }
    var from by remember { mutableStateOf(pinnedFrom) }
    var to by remember { mutableStateOf(pinnedTo) }
    var showMulti by remember { mutableStateOf(false) }

    LaunchedEffect(pinnedFrom, pinnedTo) { from = pinnedFrom; to = pinnedTo }

    val amount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
    val result = vm.convert(amount, from, to)
    val rf = remember { DecimalFormat("#,##0.00####") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionCard {
                SectionHeader("💱", "Currency Converter", "Live rates · tap swap to reverse", Gold)
                Spacer(Modifier.height(16.dp))
                FieldLabel("Amount")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                FieldLabel("From")
                Spacer(Modifier.height(6.dp))
                CurrencyPickerField("From", from) { from = it }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { val t = from; from = to; to = t }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Swap")
                    }
                    OutlinedButton(onClick = { vm.setPinnedPair(from, to) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pin Pair")
                    }
                }
                Spacer(Modifier.height(12.dp))
                FieldLabel("To")
                Spacer(Modifier.height(6.dp))
                CurrencyPickerField("To", to) { to = it }
                Spacer(Modifier.height(20.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${DecimalFormat(if (result >= 1000) "#,##0.00" else "#,##0.00####").format(result)} $to",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Green,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "1 $from = ${rf.format(vm.convert(1.0, from, to))} $to",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "1 $to = ${rf.format(vm.convert(1.0, to, from))} $from",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (from != "USD" && to != "USD" && from != "TZS") {
                        val usdEquiv = vm.convert(amount, from, "USD")
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "≈ ${DecimalFormat("#,##0.00").format(usdEquiv)} USD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    OutlinedButton(onClick = {
                        clipboard.setPrimaryClip(ClipData.newPlainText("rate", "${DecimalFormat("#,##0.00").format(result)} $to"))
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.width(18.dp))
                    }
                    OutlinedButton(onClick = {
                        val text = "${DecimalFormat("#,##0.00").format(amount)} $from = ${DecimalFormat("#,##0.00").format(result)} $to\nvia FXetcher 🇹🇿"
                        val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                        context.startActivity(Intent.createChooser(i, "Share conversion"))
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.width(18.dp))
                    }
                    Button(onClick = { vm.saveConversion(amount, from, to, result) }, modifier = Modifier.weight(1.4f)) {
                        Text("✓ Save")
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { showMulti = !showMulti }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showMulti) "🌐 Hide Multi-Currency Results" else "🌐 Show Multi-Currency Results")
                }
            }
        }

        item {
            AnimatedVisibility(visible = showMulti) {
                MultiCurrencyCard(amount = amount, from = from, rates = rates, convert = vm::convert)
            }
        }

        item { QuickAmountsCard(onPick = { amountText = it }) }

        item {
            HistoryCard(
                history = history,
                onClear = { vm.clearConversions() },
                onDelete = { vm.deleteConversion(it) }
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MultiCurrencyCard(
    amount: Double,
    from: String,
    rates: Map<String, Double>,
    convert: (Double, String, String) -> Double
) {
    val context = LocalContext.current
    SectionCard {
        SectionHeader("🌐", "Multi-Currency Results", "Same amount in all major currencies", Teal)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CurrencyMeta.MULTI_TARGETS.filter { it != from }.forEachIndexed { i, code ->
                val res = convert(amount, from, code)
                if (res <= 0) return@forEachIndexed
                val f = if (res >= 1000) DecimalFormat("#,##0.00") else DecimalFormat("#,##0.00##")
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            clipboard.setPrimaryClip(ClipData.newPlainText("conv", "${f.format(res)} $code"))
                        }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${CurrencyMeta.of(code).flag} $code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(f.format(res), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickAmountsCard(onPick: (String) -> Unit) {
    val amounts = listOf(1.0, 5.0, 10.0, 50.0, 100.0, 500.0, 1_000.0, 5_000.0, 10_000.0, 50_000.0, 100_000.0, 1_000_000.0)
    var selected by remember { mutableStateOf<Double?>(null) }
    SectionCard {
        SectionHeader("⚡", "Quick Amounts", "Tap to fill the amount field", Blue)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(amounts) { amt ->
                FilterChip(
                    selected = selected == amt,
                    onClick = {
                        selected = amt
                        onPick(if (amt >= 1000) amt.toLong().toString() else amt.toString())
                    },
                    label = { Text(chipLabel(amt)) }
                )
            }
        }
    }
}

private fun chipLabel(v: Double): String = when {
    v >= 1_000_000 -> "${(v / 1_000_000).toLong()}M"
    v >= 1_000 -> "${(v / 1_000).toLong()}K"
    else -> v.toLong().toString()
}

@Composable
private fun HistoryCard(history: List<String>, onClear: () -> Unit, onDelete: (Int) -> Unit) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("🕐 Recent Conversions", style = MaterialTheme.typography.titleMedium, color = Orange)
                Text("Last 30 conversions saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClear) { Text("Clear") }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        if (history.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No conversions yet. Tap Save to begin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                history.forEachIndexed { i, entry ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Blue.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = Blue) }
                            Spacer(Modifier.width(10.dp))
                            Text(entry, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                        IconButton(onClick = { onDelete(i) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (i < history.size - 1) HorizontalDivider()
                }
            }
        }
    }
}
