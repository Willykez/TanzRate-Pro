package com.willykez.fxetcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.willykez.fxetcher.data.CurrencyMeta

/**
 * A compact button that, when tapped, opens a searchable bottom sheet listing
 * every supported currency — the modern replacement for the old AppCompat Spinner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerField(
    label: String,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val currency = CurrencyMeta.of(selected)

    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(currency.flag, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f, fill = true)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${currency.code} · ${currency.name}", style = MaterialTheme.typography.titleSmall)
        }
    }

    if (open) {
        val sheetState = rememberModalBottomSheetState()
        var query by remember { mutableStateOf("") }
        val filtered = remember(query) {
            CurrencyMeta.ALL.filter {
                it.code.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
            }
        }
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Select Currency", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search currency or code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
                    items(filtered, key = { it.code }) { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(c.code); open = false }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.flag, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(c.code, style = MaterialTheme.typography.titleSmall)
                                Text(c.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
