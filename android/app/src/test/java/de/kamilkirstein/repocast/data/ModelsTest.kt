package de.kamilkirstein.repocast.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses episodes json written by the PC service`() {
        val podcast = json.decodeFromString<Podcast>(
            """
            {"repo": "owner/name", "title": "T", "lang": "de", "mode": "learn", "sha": "abc", "voice": "ignored",
             "episodes": [{"n": 1, "file": "01-x", "title": "Intro", "kind": "overview",
                           "commands": ["npm install"],
                           "snippets": [{"path": "src/a.ts", "startLine": 3, "endLine": 9, "note": "n"}],
                           "audio": "podcasts/owner__name/01-x.mp3", "text": "podcasts/owner__name/01-x.txt"}],
             "review": {"scores": {"tests": 1}, "good": ["g"], "improve": ["i"]}}
            """
        )
        assertEquals("abc", podcast.sha)
        assertEquals(listOf("npm install"), podcast.episodes.single().commands)
        assertEquals(9, podcast.episodes.single().snippets.single().endLine)
        assertEquals(1, podcast.review?.scores?.get("tests"))
    }

    @Test
    fun `overview podcast has no review`() {
        val podcast = json.decodeFromString<Podcast>("""{"repo": "o/n", "title": "T", "review": null}""")
        assertNull(podcast.review)
    }

    @Test
    fun `catalog entry folder`() {
        val entry = PodcastEntry(repo = "o/n", title = "T", path = "podcasts/o__n/episodes.json")
        assertEquals("podcasts/o__n", entry.folder)
    }

    @Test
    fun `finds repo links in shared text`() {
        val shared = "Check this out: https://github.com/KamilKirstein90/zahlenzug via GitHub"
        assertEquals("KamilKirstein90", GITHUB_REPO_URL.find(shared)?.groupValues?.get(1))
        assertEquals("zahlenzug", GITHUB_REPO_URL.find("https://github.com/KamilKirstein90/zahlenzug.git")?.groupValues?.get(2))
        assertTrue(GITHUB_REPO_URL.containsMatchIn("https://github.com/a/b/"))
        assertFalse(GITHUB_REPO_URL.containsMatchIn("https://gitlab.com/a/b"))
        assertFalse(GITHUB_REPO_URL.containsMatchIn("https://github.com/a"))
    }

    @Test
    fun `episode counts as finished in the last five seconds`() {
        assertTrue(EpisodeProgress(positionMs = 296_000, durationMs = 300_000).finished)
        assertFalse(EpisodeProgress(positionMs = 200_000, durationMs = 300_000).finished)
        assertFalse(EpisodeProgress(positionMs = 0, durationMs = 0).finished)
    }
}
