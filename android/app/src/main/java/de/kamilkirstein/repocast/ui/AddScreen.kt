package de.kamilkirstein.repocast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kamilkirstein.repocast.data.GITHUB_REPO_URL
import kotlinx.coroutines.launch

@Composable
fun AddScreen(vm: AppViewModel, initialUrl: String, onDone: () -> Unit, onSettings: () -> Unit) {
    val token by vm.token.collectAsStateWithLifecycle()
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var lang by rememberSaveable { mutableStateOf("de") }
    var mode by rememberSaveable { mutableStateOf("learn") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val valid = GITHUB_REPO_URL.containsMatchIn(url.trim())

    Scaffold(topBar = { BackTopBar("Repo einreichen", onDone) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (token.isBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Es ist noch kein GitHub-Token hinterlegt. Ohne Token kann die App keine Aufträge an deinen PC senden.")
                        TextButton(onClick = onSettings) { Text("Zu den Einstellungen") }
                    }
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it; error = null },
                label = { Text("GitHub-Link") },
                placeholder = { Text("https://github.com/owner/repo") },
                singleLine = true,
                isError = url.isNotBlank() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Sprache", style = MaterialTheme.typography.titleSmall)
            Choice(listOf("de" to "Deutsch", "en" to "English"), lang) { lang = it }

            Text("Modus", style = MaterialTheme.typography.titleSmall)
            Choice(listOf("overview" to "Überblick", "learn" to "Lernen"), mode) { mode = it }
            Text(
                if (mode == "learn") "Zusätzlich: Konzepte, Code-Walkthrough und ein ehrliches Code-Review (ca. 10–14 Folgen)."
                else "Was ist es, Installation, Architektur und die wichtigsten Funktionen (ca. 5–8 Folgen).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                enabled = valid && token.isNotBlank() && !sending,
                onClick = {
                    sending = true
                    scope.launch {
                        vm.submit(url, lang, mode)
                            .onSuccess { onDone() }
                            .onFailure { error = it.message ?: "Senden fehlgeschlagen" }
                        sending = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sending) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Podcast erstellen lassen")
                }
            }
            Text(
                "Dein PC muss eingeschaltet sein. Er holt den Auftrag innerhalb einer Minute ab; die Erstellung dauert je nach Repo 5–20 Minuten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Choice(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}
