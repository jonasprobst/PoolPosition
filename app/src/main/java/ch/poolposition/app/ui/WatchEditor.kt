package ch.poolposition.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.poolposition.app.model.TriggerMode
import ch.poolposition.app.model.Watch

/**
 * Add/edit dialog for a single watch. [isNew] toggles the Delete button.
 * Interval is clamped to the 15-minute minimum on save.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WatchEditor(
    initial: Watch,
    isNew: Boolean,
    onSave: (Watch) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(initial.label) }
    var url by remember { mutableStateOf(initial.url) }
    var intervalText by remember { mutableStateOf(initial.intervalMinutes.toString()) }
    var mode by remember { mutableStateOf(initial.mode) }
    var keyword by remember { mutableStateOf(initial.keyword) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var precision by remember { mutableStateOf(initial.precision) }

    val minInterval = if (precision) Watch.PRECISION_MIN_INTERVAL_MINUTES else Watch.MIN_INTERVAL_MINUTES
    val keywordRequired = mode != TriggerMode.CHANGED
    val canSave = label.isNotBlank() && url.isNotBlank() &&
        (!keywordRequired || keyword.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New watch" else "Edit watch") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { new -> intervalText = new.filter(Char::isDigit) },
                    label = { Text("Interval (minutes, min $minInterval)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Trigger when the page…")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip("changes", mode == TriggerMode.CHANGED) { mode = TriggerMode.CHANGED }
                    ModeChip("shows word", mode == TriggerMode.APPEARS) { mode = TriggerMode.APPEARS }
                    ModeChip("drops word", mode == TriggerMode.DISAPPEARS) { mode = TriggerMode.DISAPPEARS }
                }

                if (keywordRequired) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Keyword") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text(if (enabled) "Enabled" else "Disabled")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(checked = precision, onCheckedChange = { precision = it })
                    Text(if (precision) "Precision on" else "Precision off")
                }
                if (precision) {
                    Text(
                        "Exact, Doze-proof checks (down to 1 min). Uses more battery and " +
                            "shows an alarm icon; auto-stops once it fires. Turn on shortly " +
                            "before the expected change.",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val interval = intervalText.toIntOrNull()
                        ?.coerceAtLeast(minInterval)
                        ?: minInterval
                    // Changing URL/mode/keyword invalidates the stored baseline.
                    val baselineReset = url.trim() != initial.url ||
                        mode != initial.mode ||
                        keyword.trim() != initial.keyword
                    onSave(
                        initial.copy(
                            label = label.trim(),
                            url = url.trim(),
                            intervalMinutes = interval,
                            mode = mode,
                            keyword = if (keywordRequired) keyword.trim() else "",
                            enabled = enabled,
                            precision = precision,
                            lastCheckedAt = if (baselineReset) 0L else initial.lastCheckedAt,
                            lastHash = if (baselineReset) null else initial.lastHash,
                            lastKeywordPresent = if (baselineReset) null else initial.lastKeywordPresent,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isNew) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
