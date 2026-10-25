package de.kamilkirstein.repocast.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import de.kamilkirstein.repocast.data.EpisodeProgress
import de.kamilkirstein.repocast.data.PAGES_URL
import de.kamilkirstein.repocast.data.Podcast
import de.kamilkirstein.repocast.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlayerState(
    val mediaId: String? = null,
    val title: String = "",
    val podcastTitle: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
)

/**
 * UI-side handle to [PlaybackService]: exposes the player as a [StateFlow] and
 * saves the listening position so episodes resume where you stopped.
 */
class PlayerConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: Settings,
) {
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) persist()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // The previous episode played to the end: mark it as finished.
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                val previous = _state.value
                previous.mediaId?.let { id ->
                    scope.launch { settings.saveProgress(id, previous.durationMs, previous.durationMs) }
                }
            }
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { c ->
                c.addListener(listener)
                scope.launch { c.setPlaybackSpeed(settings.speed.first()) }
            }
            refresh()
        }, ContextCompat.getMainExecutor(context))

        // Position updates for the progress bar; save progress every 5 seconds.
        scope.launch {
            var ticks = 0
            while (true) {
                delay(500)
                if (controller?.isPlaying == true) {
                    refresh()
                    if (++ticks % 10 == 0) persist()
                }
            }
        }
    }

    fun play(podcast: Podcast, index: Int, progress: Map<String, EpisodeProgress>) {
        val c = controller ?: return
        val episode = podcast.episodes[index]
        if (c.currentMediaItem?.mediaId == episode.audio) {
            c.play()
            return
        }
        val items = podcast.episodes.map { ep ->
            MediaItem.Builder()
                .setMediaId(ep.audio)
                .setUri(PAGES_URL + ep.audio)
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(ep.title).setArtist(podcast.title).build()
                )
                .build()
        }
        val saved = progress[episode.audio]
        val start = if (saved != null && !saved.finished) saved.positionMs else 0
        c.setMediaItems(items, index, start)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekBack() = controller?.seekBack()

    fun seekForward() = controller?.seekForward()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        refresh()
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        scope.launch { settings.setSpeed(speed) }
    }

    fun release() {
        persist()
        controller?.release()
        controller = null
    }

    private fun refresh() {
        val c = controller ?: return
        val item = c.currentMediaItem
        _state.value = PlayerState(
            mediaId = item?.mediaId,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            podcastTitle = item?.mediaMetadata?.artist?.toString().orEmpty(),
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.coerceAtLeast(0),
            speed = c.playbackParameters.speed,
        )
    }

    private fun persist() {
        val s = _state.value
        val id = s.mediaId ?: return
        if (s.durationMs > 0) scope.launch { settings.saveProgress(id, s.positionMs, s.durationMs) }
    }
}
