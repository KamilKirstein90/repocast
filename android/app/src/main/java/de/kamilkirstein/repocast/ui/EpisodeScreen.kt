package de.kamilkirstein.repocast.ui

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kamilkirstein.repocast.R
import de.kamilkirstein.repocast.data.Podcast
import de.kamilkirstein.repocast.data.PodcastEntry
import de.kamilkirstein.repocast.data.Snippet
import kotlinx.coroutines.launch

/** Read-along view: commands to copy, the code the episode talks about, and the full script. */
@Composable
fun EpisodeScreen(vm: AppViewModel, entry: PodcastEntry, n: Int, onBack: () -> Unit) {
    val state by remember(entry.path) { vm.podcast(entry) }.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val podcast = (state as? Loadable.Ready)?.value
    val index = podcast?.episodes?.indexOfFirst { it.n == n } ?: -1
    val episode = podcast?.episodes?.getOrNull(index)

    Scaffold(topBar = { BackTopBar(episode?.title ?: "Folge $n", onBack) }) { padding ->
        if (podcast == null || episode == null) {
            Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        var script by remember(episode.text) { mutableStateOf<String?>(null) }
        LaunchedEffect(episode.text) {
            script = vm.episodeText(episode.text).getOrElse { "Text konnte nicht geladen werden: ${it.message}" }
        }

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(episode.summary, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { vm.player.play(podcast, index, progress) }) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Folge abspielen")
            }

            if (episode.commands.isNotEmpty()) {
                SectionTitle("Befehle")
                episode.commands.forEach { CommandRow(it) }
            }
            if (episode.snippets.isNotEmpty()) {
                SectionTitle("Code zum Mitlesen")
                episode.snippets.forEach { SnippetCard(vm, podcast, it) }
            }
            SectionTitle("Skript")
            Text(script ?: "…", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CommandRow(command: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            command,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(12.dp),
        )
        IconButton(onClick = {
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("command", command))) }
        }) {
            Icon(painterResource(R.drawable.ic_copy), contentDescription = "Kopieren")
        }
    }
}

@Composable
private fun SnippetCard(vm: AppViewModel, podcast: Podcast, snippet: Snippet) {
    val uriHandler = LocalUriHandler.current
    var code by remember(snippet) { mutableStateOf<String?>(null) }
    LaunchedEffect(snippet) {
        code = vm.snippetCode(podcast, snippet).getOrElse { "Code konnte nicht geladen werden: ${it.message}" }
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${snippet.path}  ·  Zeilen ${snippet.startLine}–${snippet.endLine}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (snippet.note.isNotBlank()) Text(snippet.note, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Text(code ?: "…", fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
            }
            TextButton(onClick = {
                val sha = podcast.sha.ifBlank { "HEAD" }
                uriHandler.openUri("https://github.com/${podcast.repo}/blob/$sha/${snippet.path}#L${snippet.startLine}-L${snippet.endLine}")
            }) {
                Icon(painterResource(R.drawable.ic_open), contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Auf GitHub öffnen")
            }
        }
    }
}
