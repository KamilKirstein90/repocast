package de.kamilkirstein.repocast.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

const val QUEUE_REPO = "KamilKirstein90/repocast"
const val PAGES_URL = "https://kamilkirstein90.github.io/repocast/"
private const val GITHUB_API = "https://api.github.com"

val GITHUB_REPO_URL = Regex("""https://github\.com/([\w.-]+)/([\w.-]+?)(?:\.git)?/?(?=[\s#?]|$)""")

/** All network access: podcasts from GitHub Pages, the job queue and releases from the GitHub API. */
class RepoCastApi(private val client: OkHttpClient = OkHttpClient()) {
    private val json = Json { ignoreUnknownKeys = true }

    // Pages is behind a CDN cache; a unique query string makes sure we see new podcasts right away.
    suspend fun catalog(): Catalog = json.decodeFromString(get(PAGES_URL + "catalog.json?t=${System.currentTimeMillis()}"))

    suspend fun podcast(entry: PodcastEntry): Podcast =
        json.decodeFromString(get(PAGES_URL + entry.path + "?t=${System.currentTimeMillis()}"))

    suspend fun text(path: String): String = get(PAGES_URL + path)

    suspend fun sourceLines(repo: String, sha: String, path: String): List<String> =
        get("https://raw.githubusercontent.com/$repo/$sha/$path").lines()

    suspend fun jobs(token: String?): List<Job> {
        val body = get("$GITHUB_API/repos/$QUEUE_REPO/issues?labels=repocast-request&state=all&per_page=20", token)
        return json.decodeFromString<List<IssueDto>>(body).map { issue ->
            val labels = issue.labels.map { it.name }.toSet()
            val status = when {
                "failed" in labels -> JobStatus.FAILED
                "done" in labels -> JobStatus.DONE
                "processing" in labels -> JobStatus.PROCESSING
                else -> JobStatus.QUEUED
            }
            Job(issue.number, issue.title.removePrefix("RepoCast: "), status, issue.htmlUrl)
        }
    }

    /** Creates the issue that the PC service picks up. The body is plain JSON data. */
    suspend fun requestPodcast(token: String, repoUrl: String, lang: String, mode: String) {
        val match = GITHUB_REPO_URL.find(repoUrl.trim()) ?: throw IOException("Kein gültiger GitHub-Link")
        val (owner, name) = match.destructured
        val request = buildJsonObject {
            put("url", "https://github.com/$owner/$name")
            put("lang", lang)
            put("mode", mode)
        }
        val issue = buildJsonObject {
            put("title", "RepoCast: $owner/$name")
            put("body", request.toString())
            putJsonArray("labels") { add("repocast-request") }
        }
        post("$GITHUB_API/repos/$QUEUE_REPO/issues", issue.toString(), token)
    }

    suspend fun latestRelease(): Release? {
        val release = json.decodeFromString<ReleaseDto>(get("$GITHUB_API/repos/$QUEUE_REPO/releases/latest"))
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return null
        val code = release.tagName.removePrefix("v").toIntOrNull() ?: return null
        return Release(code, apk.downloadUrl)
    }

    private suspend fun get(url: String, token: String? = null): String = execute(
        Request.Builder().url(url).withGitHubAuth(token).build()
    )

    private suspend fun post(url: String, body: String, token: String): String = execute(
        Request.Builder().url(url).withGitHubAuth(token)
            .post(body.toRequestBody("application/json".toMediaType())).build()
    )

    private fun Request.Builder.withGitHubAuth(token: String?) = apply {
        header("Accept", "application/vnd.github+json")
        if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            when {
                response.isSuccessful -> body
                response.code == 401 -> throw IOException("Token ungültig oder abgelaufen")
                response.code == 403 || response.code == 404 && request.method == "POST" ->
                    throw IOException("Token hat keine Berechtigung für Issues in $QUEUE_REPO")
                else -> throw IOException("HTTP ${response.code}")
            }
        }
    }
}
