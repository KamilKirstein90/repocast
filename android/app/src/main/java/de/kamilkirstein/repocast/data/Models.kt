package de.kamilkirstein.repocast.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** catalog.json on GitHub Pages: one entry per generated podcast. */
@Serializable
data class Catalog(val podcasts: List<PodcastEntry> = emptyList())

@Serializable
data class PodcastEntry(
    val repo: String,
    val title: String,
    val description: String = "",
    val lang: String = "de",
    val mode: String = "overview",
    val created: String = "",
    val episodes: Int = 0,
    val path: String,
) {
    /** Folder of this podcast on Pages, e.g. "podcasts/owner__repo". */
    val folder: String get() = path.substringBeforeLast('/')
}

/** episodes.json of one podcast, written by the PC service. */
@Serializable
data class Podcast(
    val repo: String,
    val title: String,
    val description: String = "",
    val lang: String = "de",
    val mode: String = "overview",
    val url: String = "",
    val sha: String = "",
    val episodes: List<Episode> = emptyList(),
    val review: Review? = null,
)

@Serializable
data class Episode(
    val n: Int,
    val file: String,
    val title: String,
    val summary: String = "",
    val kind: String = "",
    val commands: List<String> = emptyList(),
    val snippets: List<Snippet> = emptyList(),
    val audio: String,
    val text: String,
)

@Serializable
data class Snippet(val path: String, val startLine: Int, val endLine: Int, val note: String = "")

@Serializable
data class Review(
    val scores: Map<String, Int> = emptyMap(),
    val good: List<String> = emptyList(),
    val improve: List<String> = emptyList(),
)

/** A podcast request, tracked as a GitHub issue in the queue repo. */
data class Job(val number: Int, val repo: String, val status: JobStatus, val url: String)

enum class JobStatus { QUEUED, PROCESSING, DONE, FAILED }

data class Release(val versionCode: Int, val downloadUrl: String)

/** Listening progress of one episode; persisted per audio path. */
data class EpisodeProgress(val positionMs: Long, val durationMs: Long) {
    val finished: Boolean get() = durationMs > 0 && positionMs >= durationMs - 5_000
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

// GitHub REST API shapes (only the fields we use).
@Serializable
internal data class IssueDto(
    val number: Int,
    val title: String,
    val labels: List<LabelDto> = emptyList(),
    @SerialName("html_url") val htmlUrl: String,
)

@Serializable
internal data class LabelDto(val name: String)

@Serializable
internal data class ReleaseDto(@SerialName("tag_name") val tagName: String, val assets: List<AssetDto> = emptyList())

@Serializable
internal data class AssetDto(val name: String, @SerialName("browser_download_url") val downloadUrl: String)
