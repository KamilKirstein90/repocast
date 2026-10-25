package de.kamilkirstein.repocast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kamilkirstein.repocast.BuildConfig
import de.kamilkirstein.repocast.data.QUEUE_REPO

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val saved by vm.token.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    var token by rememberSaveable { mutableStateOf("") }
    var savedMessage by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(saved) { if (token.isEmpty()) token = saved }

    Scaffold(topBar = { BackTopBar("Einstellungen", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("GitHub-Token", style = MaterialTheme.typography.titleMedium)
            Text(
                "Ein Fine-grained Token, der nur Issues in $QUEUE_REPO schreiben darf. Damit sendet die App Aufträge an deinen PC.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it; savedMessage = false },
                label = { Text("Token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { vm.saveToken(token); savedMessage = true }) { Text("Speichern") }
            if (savedMessage) Text("Gespeichert ✓", color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = { uriHandler.openUri("https://github.com/settings/personal-access-tokens/new") }) {
                Text("Token auf GitHub erstellen")
            }
            Text(
                "Repository access: Only select repositories → repocast. Permissions: Issues → Read and write. Sonst nichts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("App")
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            update?.let { release ->
                Button(onClick = { uriHandler.openUri(release.downloadUrl) }) {
                    Text("Update auf Version ${release.versionCode} laden")
                }
            } ?: OutlinedButton(onClick = vm::checkForUpdate) { Text("Nach Updates suchen") }
        }
    }
}
