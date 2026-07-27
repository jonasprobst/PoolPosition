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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

// Interval presets per check mode: (minutes, label).
private val STANDARD_PRESETS = listOf(15 to "15 min", 30 to "30 min", 60 to "1 h")
private val PRECISION_PRESETS = listOf(1 to "1 min", 2 to "2 min", 5 to "5 min")
private const val STANDARD_DEFAULT = 15
private const val PRECISION_DEFAULT = 2

/**
 * Add/edit dialog for a single watch. The "How to check" segmented control picks
 * the check mode (Standard vs Precision) and swaps the interval preset chips
 * accordingly.
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
    var mode by remember { mutableStateOf(initial.mode) }
    var keyword by remember { mutableStateOf(initial.keyword) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var precision by remember { mutableStateOf(initial.precision) }
    // Snap a legacy/odd interval to a valid preset for the current mode.
    var intervalMinutes by remember {
        mutableStateOf(
            snapToPreset(initial.intervalMinutes, initial.precision),
        )
    }

    // Switching mode also moves the selected chip to that mode's valid set.
    fun selectMode(toPrecision: Boolean) {
        precision = toPrecision
        intervalMinutes = snapToPreset(intervalMinutes, toPrecision)
    }

    val presets = if (precision) PRECISION_PRESETS else STANDARD_PRESETS
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

                Text("How to check")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !precision,
                        onClick = { selectMode(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Standard") }
                    SegmentedButton(
                        selected = precision,
                        onClick = { selectMode(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Precision") }
                }
                Text(
                    if (precision) {
                        "Exact timing, even in Doze. More battery; shows an alarm icon; " +
                            "turns the watch off once it fires. Arm shortly before the change."
                    } else {
                        "Background checks, low battery. Keeps alerting on every change."
                    },
                    style = MaterialTheme.typography.labelSmall,
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (minutes, chipLabel) ->
                        FilterChip(
                            selected = intervalMinutes == minutes,
                            onClick = { intervalMinutes = minutes },
                            label = { Text(chipLabel) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text(if (enabled) "Enabled" else "Disabled")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    // Changing URL/mode/keyword invalidates the stored baseline.
                    val baselineReset = url.trim() != initial.url ||
                        mode != initial.mode ||
                        keyword.trim() != initial.keyword
                    onSave(
                        initial.copy(
                            label = label.trim(),
                            url = url.trim(),
                            intervalMinutes = intervalMinutes,
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

/** Return [minutes] if it's a valid preset for the mode, else that mode's default. */
private fun snapToPreset(minutes: Int, precision: Boolean): Int {
    val valid = (if (precision) PRECISION_PRESETS else STANDARD_PRESETS).map { it.first }
    return if (minutes in valid) minutes else if (precision) PRECISION_DEFAULT else STANDARD_DEFAULT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
