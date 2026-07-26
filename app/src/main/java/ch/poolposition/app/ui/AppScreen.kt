package ch.poolposition.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.poolposition.app.data.WatchStore
import ch.poolposition.app.model.TriggerMode
import ch.poolposition.app.model.Watch
import ch.poolposition.app.work.CheckScheduler

private class Editing(val watch: Watch, val isNew: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    onRequestNotifications: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { WatchStore(context) }
    var watches by remember { mutableStateOf(store.load()) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    fun reload() { watches = store.load() }

    fun persist(newList: List<Watch>) {
        watches = newList
        store.save(newList)
        CheckScheduler.reschedule(context)
    }

    if (showLog) {
        LogScreen(onClose = { showLog = false; reload() })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Pool Position") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Check now") },
                            onClick = {
                                menuOpen = false
                                CheckScheduler.checkNow(context)
                                android.widget.Toast.makeText(
                                    context,
                                    "Checking all watches now…",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh") },
                            onClick = { menuOpen = false; reload() },
                        )
                        DropdownMenuItem(
                            text = { Text("View log") },
                            onClick = { menuOpen = false; showLog = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Enable notifications") },
                            onClick = { menuOpen = false; onRequestNotifications() },
                        )
                        DropdownMenuItem(
                            text = { Text("Battery settings") },
                            onClick = { menuOpen = false; onOpenAppSettings() },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = Editing(
                    watch = Watch(
                        id = WatchStore.newId(),
                        label = "",
                        url = "",
                        intervalMinutes = Watch.MIN_INTERVAL_MINUTES,
                    ),
                    isNew = true,
                )
            }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
        },
    ) { padding ->
        if (watches.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No watches yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap + to watch a page for changes.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(watches, key = { it.id }) { watch ->
                    WatchCard(
                        watch = watch,
                        onClick = { editing = Editing(watch, isNew = false) },
                        onToggle = { on ->
                            persist(watches.map { if (it.id == watch.id) it.copy(enabled = on) else it })
                        },
                    )
                }
            }
        }
    }

    editing?.let { current ->
        WatchEditor(
            initial = current.watch,
            isNew = current.isNew,
            onSave = { saved ->
                val newList = if (current.isNew) {
                    watches + saved
                } else {
                    watches.map { if (it.id == saved.id) saved else it }
                }
                persist(newList)
                // Freshly saved / re-baselined watch: fetch its baseline now so the
                // next real change fires, instead of waiting for the first check.
                if (saved.enabled && saved.lastCheckedAt == 0L) {
                    CheckScheduler.baselineNow(context, saved.id)
                    android.widget.Toast.makeText(
                        context,
                        "Fetching baseline…",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
                editing = null
            },
            onDelete = {
                persist(watches.filterNot { it.id == current.watch.id })
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun WatchCard(watch: Watch, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    watch.label.ifBlank { "(no label)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    watch.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${modeLabel(watch)} · every ${watch.intervalMinutes} min",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    statusLine(watch),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(checked = watch.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun modeLabel(watch: Watch): String = when (watch.mode) {
    TriggerMode.CHANGED -> "changes"
    TriggerMode.APPEARS -> "“${watch.keyword}” appears"
    TriggerMode.DISAPPEARS -> "“${watch.keyword}” disappears"
}

private fun statusLine(watch: Watch): String {
    val checked = if (watch.lastCheckedAt == 0L) {
        "never checked"
    } else {
        "checked " + android.text.format.DateUtils.getRelativeTimeSpanString(watch.lastCheckedAt)
    }
    return checked + (watch.lastResult?.let { " · $it" } ?: "")
}
