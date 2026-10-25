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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kamilkirstein.repocast.R
import de.kamilkirstein.repocast.data.Episode
import de.kamilkirstein.repocast.data.EpisodeProgress
import de.kamilkirstein.repocast.data.PodcastEntry
import de.kamilkirstein.repocast.data.Review

@Composable
fun PodcastScreen(vm: AppViewModel, entry: PodcastEntry, onBack: () -> Unit, onOpenEpisode: (Int) -> Unit) {
    val state by remember(entry.path) { vm.podcast(entry) }.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(entry.title, onBack) }) { padding ->
        when (val s = state) {
            Loadable.Loading -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is Loadable.Failed -> Box(Modifier.padding(padding)) { ErrorText(s.message) }
            is Loadable.Ready -> {
                val podcast = s.value
                val nextIndex = podcast.episodes.indexOfFirst { progress[it.audio]?.finished != true }.coerceAtLeast(0)
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(podcast.repo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(podcast.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.player.play(podcast, nextIndex, progress) }) {
                            Icon(painterResource(R.drawable.ic_play), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (progress.keys.any { it.startsWith(entry.folder + "/") }) "Weiterhören" else "Abspielen")
                        }
                    }
                    podcast.review?.let { review -> item { ReviewCard(review) } }
                    item { SectionTitle("Folgen") }
                    itemsIndexed(podcast.episodes, key = { _, ep -> ep.audio }) { index, ep ->
                        EpisodeRow(
                            episode = ep,
                            progress = progress[ep.audio],
                            isCurrent = player.mediaId == ep.audio,
                            onPlay = { vm.player.play(podcast, index, progress) },
                            onOpen = { onOpenEpisode(ep.n) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    progress: EpisodeProgress?,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${episode.n}. ${episode.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (episode.summary.isNotBlank()) {
                    Text(episode.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                when {
                    progress?.finished == true -> Text("✓ gehört", style = MaterialTheme.typography.labelSmall)
                    progress != null && progress.positionMs > 0 -> {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(onClick = onPlay) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = "Abspielen")
            }
        }
    }
}

private val reviewLabels = mapOf(
    "structure" to "Struktur",
    "readability" to "Lesbarkeit",
    "tests" to "Tests",
    "errorHandling" to "Fehlerbehandlung",
    "security" to "Sicherheit",
    "dependencies" to "Abhängigkeiten",
)

/** Code quality review from Learn mode: scores 1–5 plus what is good and what could be better. */
@Composable
private fun ReviewCard(review: Review) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Code-Review", style = MaterialTheme.typography.titleMedium)
            review.scores.forEach { (key, score) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reviewLabels[key] ?: key, Modifier.width(140.dp), style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { score / 5f }, modifier = Modifier.weight(1f))
                    Text("  $score/5", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (review.good.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Gut gemacht", style = MaterialTheme.typography.titleSmall)
                review.good.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
            if (review.improve.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Verbesserungspotenzial", style = MaterialTheme.typography.titleSmall)
                review.improve.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
