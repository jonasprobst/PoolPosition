package ch.poolposition.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ch.poolposition.app.core.Logger

/** Full-screen diagnostic log viewer with refresh / clear / close. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(Logger.read(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Log") },
                navigationIcon = { TextButton(onClick = onClose) { Text("Close") } },
                actions = {
                    TextButton(onClick = { text = Logger.read(context) }) { Text("Refresh") }
                    TextButton(onClick = { Logger.clear(context); text = "" }) { Text("Clear") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = text.ifBlank { "No log entries yet." },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
