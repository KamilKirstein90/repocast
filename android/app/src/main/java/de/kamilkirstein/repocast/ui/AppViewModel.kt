package de.kamilkirstein.repocast.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.kamilkirstein.repocast.BuildConfig
import de.kamilkirstein.repocast.data.Catalog
import de.kamilkirstein.repocast.data.EpisodeProgress
import de.kamilkirstein.repocast.data.Job
import de.kamilkirstein.repocast.data.JobStatus
import de.kamilkirstein.repocast.data.Podcast
import de.kamilkirstein.repocast.data.PodcastEntry
import de.kamilkirstein.repocast.data.Release
import de.kamilkirstein.repocast.data.RepoCastApi
import de.kamilkirstein.repocast.data.Settings
import de.kamilkirstein.repocast.data.Snippet
import de.kamilkirstein.repocast.player.PlayerConnection
import de.kamilkirstein.repocast.ui.theme.AppStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val message: String) : Loadable<Nothing>
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val api = RepoCastApi()
    private val settings = Settings(app)
    val player = PlayerConnection(app, viewModelScope, settings)

    val catalog = MutableStateFlow<Loadable<Catalog>>(Loadable.Loading)
    val jobs = MutableStateFlow<List<Job>>(emptyList())
    val refreshing = MutableStateFlow(false)
    val update = MutableStateFlow<Release?>(null)
    val token: StateFlow<String> = settings.token.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val progress: StateFlow<Map<String, EpisodeProgress>> =
        settings.progress.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    /** The chosen look; null until it has been read from storage, so the app never flashes the wrong one. */
    val style: StateFlow<AppStyle?> = settings.style.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val podcasts = mutableMapOf<String, MutableStateFlow<Loadable<Podcast>>>()

    init {
        refresh()
        checkForUpdate()
        // While a request is queued or running, watch it and reload the library when it is done.
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (jobs.value.any { it.status == JobStatus.QUEUED || it.status == JobStatus.PROCESSING }) {
                    val before = jobs.value.count { it.status == JobStatus.DONE }
                    loadJobs()
                    if (jobs.value.count { it.status == JobStatus.DONE } > before) refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            catalog.value = load { api.catalog() }
            podcasts.clear()
            loadJobs()
            refreshing.value = false
        }
    }

    fun podcast(entry: PodcastEntry): StateFlow<Loadable<Podcast>> = podcasts.getOrPut(entry.path) {
        MutableStateFlow<Loadable<Podcast>>(Loadable.Loading).also { flow ->
            viewModelScope.launch { flow.value = load { api.podcast(entry) } }
        }
    }

    suspend fun episodeText(path: String): Result<String> = runCatching { api.text(path) }

    /** The exact source lines a snippet points to, loaded from the analyzed commit. */
    suspend fun snippetCode(podcast: Podcast, snippet: Snippet): Result<String> = runCatching {
        val lines = api.sourceLines(podcast.repo, podcast.sha.ifBlank { "HEAD" }, snippet.path)
        val from = (snippet.startLine - 1).coerceIn(0, lines.size)
        val to = snippet.endLine.coerceIn(from, lines.size)
        lines.subList(from, to).joinToString("\n").trimIndent()
    }

    suspend fun submit(url: String, lang: String, mode: String): Result<Unit> = runCatching {
        api.requestPodcast(token.value, url, lang, mode)
        loadJobs()
    }

    fun saveToken(value: String) {
        viewModelScope.launch { settings.setToken(value) }
    }

    fun setStyle(style: AppStyle) {
        viewModelScope.launch { settings.setStyle(style) }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            update.value = runCatching { api.latestRelease() }.getOrNull()
                ?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        }
    }

    private suspend fun loadJobs() {
        runCatching { api.jobs(token.value.ifBlank { null }) }.onSuccess { jobs.value = it }
    }

    private suspend fun <T> load(block: suspend () -> T): Loadable<T> = runCatching { block() }.fold(
        onSuccess = { Loadable.Ready(it) },
        onFailure = { Loadable.Failed(it.message ?: "Unbekannter Fehler") },
    )

    override fun onCleared() {
        player.release()
    }
}
