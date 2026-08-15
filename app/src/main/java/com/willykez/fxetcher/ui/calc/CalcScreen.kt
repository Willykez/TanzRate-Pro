package com.willykez.fxetcher.ui.calc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.willykez.fxetcher.data.CurrencyMeta
import com.willykez.fxetcher.ui.FxViewModel
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
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcScreen(vm: FxViewModel) {
    val strings = LocalStrings.current
    val rates by vm.rates.collectAsState()
    val history by vm.calcHistory.collectAsState()
    val context = LocalContext.current

    var mode by remember { mutableStateOf(0) } // 0 = keypad, 1 = split
    var selectedCode by remember { mutableStateOf("USD") }
    var inputIsTzs by remember { mutableStateOf(false) }
    var expr by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = mode == 0, onClick = { mode = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text("🧮 ${strings.keypad}")
                }
                SegmentedButton(selected = mode == 1, onClick = { mode = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text("🍰 ${strings.split}")
                }
            }
        }

        if (mode == 0) {
            item {
                DisplayCard(
                    selectedCode = selectedCode, inputIsTzs = inputIsTzs, expr = expr, rates = rates,
                    fmtTzs = vm.fmtTzs::format,
                    onClear = { expr = "" }
                )
            }
            item {
                CurrencyChipsRow(selectedCode = selectedCode) { selectedCode = it }
            }
            item {
                DirectionRow(selectedCode = selectedCode, inputIsTzs = inputIsTzs) { inputIsTzs = !inputIsTzs }
            }
            item {
                Keypad(
                    onKey = { key ->
                        expr = applyKey(expr, key) { finalExpr ->
                            val value = evalExpr(finalExpr)
                            val rate = rates[selectedCode]
                            if (rate != null && rate != 0.0) {
                                val result = if (inputIsTzs) value / rate else value * rate
                                val unit = if (inputIsTzs) selectedCode else "TZS"
                                vm.saveCalc("$finalExpr = ${DecimalFormat("#,##0.########").format(result)} $unit")
                            }
                        }
                    }
                )
            }
            item {
                RateReferenceCard(rates = rates, fmt = vm.fmtTzs::format)
            }
            item {
                CalcHistoryCard(history = history, onClear = { vm.clearCalcHistory() })
            }
        } else {
            item { SplitCalculator(rates = rates, fmt = vm.fmtTzs::format) }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

private fun applyKey(expr: String, key: String, onEquals: (String) -> Unit): String = when (key) {
    "⌫" -> if (expr.isNotEmpty()) expr.dropLast(1) else expr
    "AC" -> ""
    "=" -> {
        val result = runCatching { evalExpr(expr) }.getOrNull()
        if (result != null) {
            onEquals(expr)
            DecimalFormat("#,##0.########").format(result)
        } else expr
    }
    "%" -> runCatching { (evalExpr(expr) / 100.0).toString() }.getOrDefault(expr)
    "×" -> expr + "×"
    else -> expr + key
}

private fun evalExpr(raw: String): Double {
    val s = raw.replace(",", "").replace("×", "*").replace("÷", "/")
    fun eval(str: String): Double {
        for (i in str.length - 1 downTo 1) {
            val c = str[i]
            if ((c == '+' || c == '-') && str[i - 1] != 'e' && str[i - 1] != 'E') {
                val l = str.substring(0, i)
                val r = str.substring(i + 1)
                if (r.isNotEmpty()) return if (c == '+') eval(l) + eval(r) else eval(l) - eval(r)
            }
        }
        for (i in str.length - 1 downTo 0) {
            val c = str[i]
            if (c == '*' || c == '/') {
                val l = eval(str.substring(0, i))
                val r = eval(str.substring(i + 1))
                return if (c == '*') l * r else if (r != 0.0) l / r else 0.0
            }
        }
        return str.trim().toDoubleOrNull() ?: 0.0
    }
    return runCatching { eval(s) }.getOrDefault(0.0)
}

@Composable
private fun DisplayCard(
    selectedCode: String, inputIsTzs: Boolean, expr: String,
    rates: Map<String, Double>, fmtTzs: (Double) -> String, onClear: () -> Unit
) {
    val currency = CurrencyMeta.of(selectedCode)
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${currency.code}  ${currency.flag}", style = MaterialTheme.typography.titleSmall, color = Gold, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClear) { Text("AC", color = Red) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            expr.ifEmpty { "0" },
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        val value = runCatching { evalExpr(expr.ifEmpty { "0" }) }.getOrDefault(0.0)
        val rate = rates[selectedCode]
        val (result, unit) = if (rate != null && rate != 0.0) {
            if (inputIsTzs) value / rate to selectedCode else value * rate to "TZS"
        } else 0.0 to "—"
        Text(
            if (rate != null) "${DecimalFormat(if (result >= 1000) "#,##0.00" else "#,##0.0000").format(result)} $unit" else "—",
            style = MaterialTheme.typography.headlineMedium,
            color = Green,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )
        if (rate != null) {
            Text(
                "1 $selectedCode = ${fmtTzs(rate)} TZS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CurrencyChipsRow(selectedCode: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CurrencyMeta.CALC_QUICK) { c ->
            val currency = CurrencyMeta.of(c)
            FilterChip(
                selected = selectedCode == c,
                onClick = { onSelect(c) },
                label = { Text("${currency.flag} $c") }
            )
        }
    }
}

@Composable
private fun DirectionRow(selectedCode: String, inputIsTzs: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Button(onClick = onToggle, colors = ButtonDefaults.buttonColors(containerColor = Blue.copy(alpha = 0.16f), contentColor = Blue)) {
            Text(if (inputIsTzs) "$selectedCode ← TZS" else "$selectedCode → TZS")
        }
    }
}

@Composable
private fun Keypad(onKey: (String) -> Unit) {
    val keys = listOf(
        listOf("AC", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=", "=")
    )
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.take(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        KeypadButton(k, Modifier.weight(1f)) { onKey(k) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeypadButton("0", Modifier.weight(2f)) { onKey("0") }
                KeypadButton(".", Modifier.weight(1f)) { onKey(".") }
                KeypadButton("=", Modifier.weight(1f), highlight = true) { onKey("=") }
            }
        }
    }
}

@Composable
private fun KeypadButton(label: String, modifier: Modifier = Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    val bg = when {
        highlight -> Gold.copy(alpha = 0.25f)
        label == "AC" -> Red.copy(alpha = 0.18f)
        label in setOf("÷", "×", "-", "+", "%") -> Blue.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        highlight -> Gold
        label == "AC" -> Red
        label in setOf("÷", "×", "-", "+", "%") -> Blue
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RateReferenceCard(rates: Map<String, Double>, fmt: (Double) -> String) {
    val strings = LocalStrings.current
    SectionCard {
        SectionHeader("📋", strings.rateRefTitle, strings.rateRefSubtitle, Teal)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        CurrencyMeta.REF_CODES.forEachIndexed { i, c ->
            val r = rates[c] ?: return@forEachIndexed
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${CurrencyMeta.of(c).flag} $c", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("1 = ${fmt(r)} TZS", style = MaterialTheme.typography.bodyMedium, color = accentFor(i), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CalcHistoryCard(history: List<String>, onClear: () -> Unit) {
    val strings = LocalStrings.current
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("🕐 ${strings.calcHistoryTitle}", style = MaterialTheme.typography.titleMedium, color = Orange)
            }
            TextButton(onClick = onClear) { Text(strings.clear) }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        if (history.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column {
                history.forEach { entry ->
                    Text(entry, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                    HorizontalDivider()
                }
            }
        }
    }
}

// ── New feature: split a TZS amount evenly across several currencies ────────
@Composable
private fun SplitCalculator(rates: Map<String, Double>, fmt: (Double) -> String) {
    val strings = LocalStrings.current
    var amountText by remember { mutableStateOf("100000") }
    var selected by remember { mutableStateOf(setOf("USD", "EUR", "KES")) }

    SectionCard {
        SectionHeader("🍰", strings.splitTitle, strings.splitSubtitle, Purple)
        Spacer(Modifier.height(14.dp))
        androidx.compose.material3.OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() } },
            label = { Text("Total amount (TZS)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(14.dp))
        Text(strings.splitBetween, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
        gridItems(CurrencyMeta.MULTI_TARGETS) { c ->
                val isSel = selected.contains(c)
                FilterChip(
                    selected = isSel,
                    onClick = { selected = if (isSel) selected - c else selected + c },
                    label = { Text(c, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))

        val total = amountText.toDoubleOrNull() ?: 0.0
        val each = if (selected.isNotEmpty()) total / selected.size else 0.0
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            selected.sorted().forEach { c ->
                val rate = rates[c]
                val converted = rate?.let { each / it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${CurrencyMeta.of(c).flag} $c", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        converted?.let { DecimalFormat("#,##0.00").format(it) } ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                }
            }
        }
        if (selected.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "${fmt(each)} TZS per currency (÷ ${selected.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
