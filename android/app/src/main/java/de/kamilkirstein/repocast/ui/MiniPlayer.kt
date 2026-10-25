package de.kamilkirstein.repocast.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.kamilkirstein.repocast.R
import de.kamilkirstein.repocast.player.PlayerConnection
import de.kamilkirstein.repocast.player.PlayerState

private val speeds = listOf(1f, 1.25f, 1.5f, 1.75f, 2f)

@Composable
fun MiniPlayer(state: PlayerState, player: PlayerConnection) {
    // While dragging, show the drag position instead of the live one.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1)

    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp)) {
            Slider(
                value = dragging ?: (state.positionMs.toFloat() / duration),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { player.seekTo((it * duration).toLong()) }
                    dragging = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)} · ${state.podcastTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = player::seekBack) {
                    Icon(painterResource(R.drawable.ic_replay), contentDescription = "15 s zurück")
                }
                FilledIconButton(onClick = player::togglePlay) {
                    Icon(
                        painterResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = if (state.isPlaying) "Pause" else "Abspielen",
                    )
                }
                IconButton(onClick = player::seekForward) {
                    Icon(painterResource(R.drawable.ic_forward), contentDescription = "15 s vor")
                }
                TextButton(onClick = {
                    val next = speeds[(speeds.indexOf(state.speed) + 1) % speeds.size]
                    player.setSpeed(next)
                }) {
                    Text("${state.speed}×".replace(".0×", "×"))
                }
            }
        }
    }
}
