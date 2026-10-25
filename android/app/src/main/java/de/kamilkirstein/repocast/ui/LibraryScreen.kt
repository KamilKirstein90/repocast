package de.kamilkirstein.repocast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.kamilkirstein.repocast.R
import de.kamilkirstein.repocast.data.EpisodeProgress
import de.kamilkirstein.repocast.data.Job
import de.kamilkirstein.repocast.data.JobStatus
import de.kamilkirstein.repocast.data.PodcastEntry
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: AppViewModel, onOpen: (Int) -> Unit, onAdd: () -> Unit, onSettings: () -> Unit) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RepoCast") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Einstellungen")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text("Repo") },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                update?.let { release ->
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Neue App-Version verfügbar", Modifier.weight(1f))
                                TextButton(onClick = { uriHandler.openUri(release.downloadUrl) }) { Text("Laden") }
                            }
                        }
                    }
                }

                val active = jobs.filter { it.status != JobStatus.DONE }.take(5)
                if (active.isNotEmpty()) {
                    item { SectionTitle("Aufträge") }
                    items(active, key = { "job${it.number}" }) { job ->
                        JobRow(job, onClick = { uriHandler.openUri(job.url) })
                    }
                    item { SectionTitle("Podcasts") }
                }

                when (val c = catalog) {
                    Loadable.Loading -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Loadable.Failed -> item { ErrorText("Bibliothek konnte nicht geladen werden: ${c.message}") }
                    is Loadable.Ready -> {
                        val sorted = c.value.podcasts.withIndex().sortedByDescending { it.value.created }
                        if (sorted.isEmpty()) {
                            item { Text("Noch keine Podcasts. Tippe auf „Repo“, um einen GitHub-Link einzureichen.") }
                        }
                        items(sorted, key = { it.value.path }) { (index, entry) ->
                            PodcastCard(entry, progress, onClick = { onOpen(index) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: Job, onClick: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (job.status == JobStatus.QUEUED || job.status == JobStatus.PROCESSING) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(job.repo, style = MaterialTheme.typography.titleSmall)
                Text(
                    when (job.status) {
                        JobStatus.QUEUED -> "Wartet – der PC holt den Auftrag innerhalb einer Minute ab"
                        JobStatus.PROCESSING -> "Wird erstellt … (dauert einige Minuten)"
                        JobStatus.FAILED -> "Fehlgeschlagen – tippen für Details"
                        JobStatus.DONE -> "Fertig"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (job.status == JobStatus.FAILED) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PodcastCard(entry: PodcastEntry, progress: Map<String, EpisodeProgress>, onClick: () -> Unit) {
    val finished = progress.count { (audio, p) -> audio.startsWith(entry.folder + "/") && p.finished }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${entry.repo} · ${entry.lang.uppercase()} · ${modeLabel(entry.mode)} · ${entry.episodes} Folgen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(entry.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            }
            if (entry.episodes > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { finished.toFloat() / entry.episodes },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("$finished von ${entry.episodes} gehört", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

fun modeLabel(mode: String) = if (mode == "learn") "Lernen" else "Überblick"
