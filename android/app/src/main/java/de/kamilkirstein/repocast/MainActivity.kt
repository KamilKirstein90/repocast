package de.kamilkirstein.repocast

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.kamilkirstein.repocast.data.GITHUB_REPO_URL
import de.kamilkirstein.repocast.ui.RepoCastApp
import de.kamilkirstein.repocast.ui.theme.RepoCastTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** A GitHub link shared into the app ("Share → RepoCast"), consumed by the add screen. */
    private val sharedUrl = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedUrl.value = intent.sharedGitHubUrl()
        setContent {
            RepoCastTheme {
                RepoCastApp(sharedUrl)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedUrl.value = intent.sharedGitHubUrl()
    }
}

private fun Intent.sharedGitHubUrl(): String? =
    if (action == Intent.ACTION_SEND) getStringExtra(Intent.EXTRA_TEXT)?.let { GITHUB_REPO_URL.find(it)?.value } else null
