package de.kamilkirstein.repocast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kamilkirstein.repocast.R
import de.kamilkirstein.repocast.data.EpisodeProgress
import de.kamilkirstein.repocast.data.Job
import de.kamilkirstein.repocast.data.JobStatus
import de.kamilkirstein.repocast.data.PodcastEntry
import de.kamilkirstein.repocast.data.QUEUE_REPO
import de.kamilkirstein.repocast.ui.theme.AppStyle
import de.kamilkirstein.repocast.ui.theme.LocalAppStyle
import de.kamilkirstein.repocast.ui.theme.LocalStyleExtras
import de.kamilkirstein.repocast.ui.theme.Logo
import kotlin.math.absoluteValue

private const val FAILED_ISSUES_URL = "https://github.com/$QUEUE_REPO/issues?q=is%3Aissue+label%3Afailed"

/** One podcast in the library with its listening progress. */
private data class LibraryItem(val index: Int, val entry: PodcastEntry, val finished: Int, val started: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: AppViewModel, onOpen: (Int) -> Unit, onAdd: () -> Unit, onSettings: () -> Unit) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val style = LocalAppStyle.current
    val uriHandler = LocalUriHandler.current

    val items = (catalog as? Loadable.Ready)?.value?.podcasts.orEmpty()
        .withIndex()
        .sortedByDescending { it.value.created }
        .map { (index, entry) ->
            val mine = progress.filterKeys { it.startsWith(entry.folder + "/") }
            LibraryItem(index, entry, mine.values.count { it.finished }, mine.isNotEmpty())
        }
    // Continue the podcast you are in the middle of, otherwise suggest the newest one.
    val current = items.firstOrNull { it.started && it.finished < it.entry.episodes } ?: items.firstOrNull()
    val running = jobs.filter { it.status == JobStatus.QUEUED || it.status == JobStatus.PROCESSING }
    // A failed request only matters while there is still no podcast for that repo.
    val podcastRepos = items.map { it.entry.repo.lowercase() }.toSet()
    val failed = jobs.count { it.status == JobStatus.FAILED && it.repo.lowercase() !in podcastRepos }

    Scaffold(
        floatingActionButton = { AddButton(style, onAdd) },
        floatingActionButtonPosition = if (style == AppStyle.DAEMMERUNG) FabPosition.Center else FabPosition.End,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (style == AppStyle.LEINEN) 2 else 1),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(if (style == AppStyle.NACHTRADIO) 8.dp else 14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                full("header") { Header(style, onSettings) }
                if (style == AppStyle.LEINEN) {
                    full("title") {
                        Column {
                            Text("Deine Podcasts", fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
                            Text(
                                "${items.size} Serien · ${items.sumOf { it.entry.episodes }} Folgen",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                update?.let { release ->
                    full("update") {
                        StatusLine(
                            text = "Neue App-Version verfügbar",
                            action = "Laden",
                            dot = MaterialTheme.colorScheme.primary,
                            onClick = { uriHandler.openUri(release.downloadUrl) },
                        )
                    }
                }
                items(running, key = { "job${it.number}" }, span = { GridItemSpan(maxLineSpan) }) { job ->
                    RunningJob(job, onClick = { uriHandler.openUri(job.url) })
                }
                if (failed > 0 && style != AppStyle.DAEMMERUNG) {
                    full("failed") {
                        StatusLine(
                            text = if (failed == 1) "1 Auftrag fehlgeschlagen" else "$failed Aufträge fehlgeschlagen",
                            action = "Ansehen",
                            dot = MaterialTheme.colorScheme.error,
                            onClick = { uriHandler.openUri(FAILED_ISSUES_URL) },
                        )
                    }
                }

                when (val c = catalog) {
                    Loadable.Loading -> full("loading") {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Loadable.Failed -> full("error") { ErrorText("Bibliothek konnte nicht geladen werden: ${c.message}") }
                    is Loadable.Ready -> {
                        if (items.isEmpty()) {
                            full("empty") {
                                Text(
                                    "Noch keine Podcasts. Tippe unten auf den Knopf, um einen GitHub-Link einzureichen.",
                                    modifier = Modifier.padding(vertical = 24.dp),
                                )
                            }
                        }
                        current?.let { item ->
                            full("hero") { Hero(vm, style, item, progress, onOpen = { onOpen(item.index) }) }
                        }
                        if (items.isNotEmpty() && style != AppStyle.LEINEN) {
                            full("library") {
                                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Bibliothek", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    if (failed > 0 && style == AppStyle.DAEMMERUNG) {
                                        TextButton(onClick = { uriHandler.openUri(FAILED_ISSUES_URL) }) {
                                            Text("$failed fehlgeschlagen")
                                        }
                                    }
                                }
                            }
                        }
                        items(items, key = { it.entry.path }) { item ->
                            when (style) {
                                AppStyle.NACHTRADIO -> PodcastRow(item, onClick = { onOpen(item.index) })
                                AppStyle.LEINEN -> PodcastTile(item, onClick = { onOpen(item.index) })
                                AppStyle.DAEMMERUNG -> PodcastCard(item, onClick = { onOpen(item.index) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.full(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun Header(style: AppStyle, onSettings: () -> Unit) {
    Row(
        Modifier.padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Logo(style, Modifier.size(if (style == AppStyle.DAEMMERUNG) 40.dp else 36.dp))
        // Leinen shows its big "Deine Podcasts" title below instead of a wordmark.
        if (style == AppStyle.LEINEN) Spacer(Modifier.weight(1f))
        else Text("RepoCast", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        IconButton(
            onClick = onSettings,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = "Einstellungen")
        }
    }
}

@Composable
private fun AddButton(style: AppStyle, onAdd: () -> Unit) {
    val icon = @Composable { Icon(painterResource(R.drawable.ic_add), contentDescription = null) }
    when (style) {
        AppStyle.NACHTRADIO -> ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = icon,
            text = { Text("Neuer Podcast", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
        )
        AppStyle.LEINEN -> FloatingActionButton(
            onClick = onAdd,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.size(64.dp),
        ) { Icon(painterResource(R.drawable.ic_add), contentDescription = "Neuer Podcast") }
        AppStyle.DAEMMERUNG -> Button(
            onClick = onAdd,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
        ) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text("Repo als Podcast", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/** A compact one-line notice (update available, failed requests). */
@Composable
private fun StatusLine(text: String, action: String, dot: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** A request that is waiting or being built; disappears once the podcast is there. */
@Composable
private fun RunningJob(job: Job, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.secondary) }
        Column(Modifier.weight(1f)) {
            Text("${job.repo} wird erstellt …", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (job.status == JobStatus.QUEUED) "Wartet, bis der PC den Auftrag abholt" else "Dauert einige Minuten",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The big "continue listening" card; plays the next unfinished episode directly. */
@Composable
private fun Hero(
    vm: AppViewModel,
    style: AppStyle,
    item: LibraryItem,
    progress: Map<String, EpisodeProgress>,
    onOpen: () -> Unit,
) {
    val podcast by remember(item.entry.path) { vm.podcast(item.entry) }.collectAsStateWithLifecycle()
    val loaded = (podcast as? Loadable.Ready)?.value
    val next = loaded?.episodes?.indexOfFirst { progress[it.audio]?.finished != true }?.coerceAtLeast(0)
    val play = { if (loaded != null && next != null) vm.player.play(loaded, next, progress) else onOpen() }
    val label = if (item.started) "Weiterhören" else "Neu"
    val subtitle = "Folge ${(next ?: item.finished) + 1} von ${item.entry.episodes} · " +
        "${modeLabel(item.entry.mode)} · ${item.entry.lang.uppercase()}"
    val colors = MaterialTheme.colorScheme

    if (style == AppStyle.LEINEN) {
        // A dark "now playing" strip, like a docked mini player.
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.inverseSurface)
                .clickable(onClick = onOpen)
                .padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Cover(item.entry, Modifier.size(44.dp), corner = 12.dp, textSize = 15.sp)
            Column(Modifier.weight(1f)) {
                Text(shortTitle(item.entry), color = colors.inverseOnSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$label · Folge ${(next ?: item.finished) + 1}", color = colors.inverseOnSurface.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            }
            PlayButton(play, colors.inverseOnSurface, colors.inverseSurface, 44.dp)
        }
        return
    }

    val gradient = LocalStyleExtras.current.heroGradient
    Column(
        Modifier.fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(if (style == AppStyle.DAEMMERUNG) 28.dp else 24.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onOpen)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (style == AppStyle.DAEMMERUNG) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onPrimaryContainer,
                modifier = Modifier.clip(CircleShape).background(colors.primary.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.entry.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Waveform(colors.outline, Modifier.weight(1f).height(36.dp))
                Spacer(Modifier.width(16.dp))
                PlayButton(play, colors.primary, colors.onPrimary, 56.dp)
            }
        } else {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = colors.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Cover(item.entry, Modifier.size(72.dp), corner = 16.dp, textSize = 26.sp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlayButton(play, colors.primary, colors.onPrimary, 52.dp)
                ListenProgress(item, Modifier.weight(1f))
                Text("${item.finished} / ${item.entry.episodes}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit, container: Color, content: Color, size: androidx.compose.ui.unit.Dp) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
        modifier = Modifier.size(size),
    ) { Icon(painterResource(R.drawable.ic_play), contentDescription = "Abspielen") }
}

/** Decorative bars standing in for an audio waveform. */
@Composable
private fun Waveform(color: Color, modifier: Modifier) {
    val heights = listOf(10, 22, 30, 16, 26, 36, 20, 12, 28, 18, 32, 14, 24, 10, 20, 30, 16, 8, 22, 12)
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        heights.forEach { h -> Box(Modifier.width(4.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(color)) }
    }
}

@Composable
private fun ListenProgress(item: LibraryItem, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { if (item.entry.episodes > 0) item.finished.toFloat() / item.entry.episodes else 0f },
        modifier = modifier.height(4.dp),
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        drawStopIndicator = {},
    )
}

/** A colored square with the repo's initials; the color is stable per repo. */
@Composable
private fun Cover(entry: PodcastEntry, modifier: Modifier, corner: androidx.compose.ui.unit.Dp, textSize: androidx.compose.ui.unit.TextUnit) {
    val color = coverColor(entry)
    Box(modifier.clip(RoundedCornerShape(corner)).background(color.background), contentAlignment = Alignment.Center) {
        Text(initials(entry), color = color.content, fontWeight = FontWeight.ExtraBold, fontSize = textSize)
    }
}

@Composable
private fun coverColor(entry: PodcastEntry) = LocalStyleExtras.current.covers.let { it[entry.repo.hashCode().absoluteValue % it.size] }

private fun repoName(entry: PodcastEntry) = entry.repo.substringAfter('/')

private fun initials(entry: PodcastEntry) = repoName(entry).filter { it.isLetterOrDigit() }.take(2)
    .replaceFirstChar { it.uppercase() }

/** "Slugify: Small String, Big Details" → "Slugify". */
private fun shortTitle(entry: PodcastEntry) = entry.title.split(":", " – ", " - ").first().trim()

private fun meta(item: LibraryItem) =
    "${modeLabel(item.entry.mode)} · ${item.entry.lang.uppercase()} · ${item.finished}/${item.entry.episodes} gehört"

// Nachtradio: a quiet list.
@Composable
private fun PodcastRow(item: LibraryItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Cover(item.entry, Modifier.size(56.dp), corner = 14.dp, textSize = 20.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${item.entry.repo} · ${meta(item)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            ListenProgress(item, Modifier.fillMaxWidth().padding(top = 2.dp))
        }
    }
}

// Leinen: square cover tiles in a grid.
@Composable
private fun PodcastTile(item: LibraryItem, onClick: () -> Unit) {
    val color = coverColor(item.entry)
    Column(Modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(color.background).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(repoName(item.entry), color = color.content.copy(alpha = 0.85f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(shortTitle(item.entry), color = color.content, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(meta(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Dämmerung: soft cards.
@Composable
private fun PodcastCard(item: LibraryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Cover(item.entry, Modifier.size(52.dp), corner = 16.dp, textSize = 18.sp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(meta(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun modeLabel(mode: String) = if (mode == "learn") "Lernen" else "Überblick"
