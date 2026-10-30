package de.kamilkirstein.repocast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import de.kamilkirstein.repocast.ui.theme.AppStyle
import de.kamilkirstein.repocast.ui.theme.LocalAppStyle
import de.kamilkirstein.repocast.ui.theme.Logo
import de.kamilkirstein.repocast.ui.theme.colorScheme
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
    val style = LocalAppStyle.current
    var token by rememberSaveable { mutableStateOf("") }
    var savedMessage by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(saved) { if (token.isEmpty()) token = saved }

    Scaffold(topBar = { BackTopBar("Einstellungen", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Design", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppStyle.entries.forEach { option ->
                    StyleOption(option, selected = option == style, onClick = { vm.setStyle(option) }, Modifier.weight(1f))
                }
            }

            Text("GitHub-Token", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
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

/** A small preview of a style in its own colors; tapping it switches the whole app. */
@Composable
private fun StyleOption(option: AppStyle, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = colorScheme(option)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .border(
                if (selected) 2.5.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .background(colors.background)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Logo(option, Modifier.size(44.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(colors.primary, colors.secondary, colors.surfaceContainerHigh).forEach {
                Box(Modifier.size(12.dp).clip(CircleShape).background(it))
            }
        }
        Text(option.label, color = colors.onBackground, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(
            option.tagline,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}
