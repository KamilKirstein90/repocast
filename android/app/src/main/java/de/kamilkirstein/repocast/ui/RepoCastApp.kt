package de.kamilkirstein.repocast.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.kamilkirstein.repocast.data.PodcastEntry
import de.kamilkirstein.repocast.ui.theme.RepoCastTheme
import kotlinx.coroutines.flow.MutableStateFlow

/** Applies the chosen style and plays the intro once per app start, then shows the screens. */
@Composable
fun RepoCastApp(sharedUrl: MutableStateFlow<String?>) {
    val vm: AppViewModel = viewModel()
    val style by vm.style.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val activity = LocalActivity.current as? ComponentActivity
    val current = style ?: return

    // Status bar icons must contrast with the style's background, not the system theme.
    LaunchedEffect(current) {
        val bars = if (current.dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
    }

    RepoCastTheme(current) {
        Crossfade(showSplash, animationSpec = tween(400), label = "splash") { splash ->
            if (splash) SplashScreen(current, onFinished = { showSplash = false })
            else Screens(vm, sharedUrl)
        }
    }
}

/** Navigation between the screens, with the mini player docked at the bottom. */
@Composable
private fun Screens(vm: AppViewModel, sharedUrl: MutableStateFlow<String?>) {
    val nav = rememberNavController()
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val shared by sharedUrl.collectAsStateWithLifecycle()
    val showPlayer = playerState.mediaId != null

    LaunchedEffect(shared) {
        if (shared != null) nav.navigate("add")
    }

    fun entry(index: Int): PodcastEntry? = (catalog as? Loadable.Ready)?.value?.podcasts?.getOrNull(index)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (showPlayer) MiniPlayer(playerState, vm.player) },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "library",
            modifier = Modifier
                .padding(inner)
                .then(if (showPlayer) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
        ) {
            composable("library") {
                LibraryScreen(
                    vm = vm,
                    onOpen = { nav.navigate("podcast/$it") },
                    onAdd = { nav.navigate("add") },
                    onSettings = { nav.navigate("settings") },
                )
            }
            composable("podcast/{i}", listOf(navArgument("i") { type = NavType.IntType })) { back ->
                val i = back.arguments?.getInt("i") ?: 0
                entry(i)?.let { e ->
                    PodcastScreen(vm, e, onBack = nav::popBackStack, onOpenEpisode = { n -> nav.navigate("episode/$i/$n") })
                }
            }
            composable(
                "episode/{i}/{n}",
                listOf(navArgument("i") { type = NavType.IntType }, navArgument("n") { type = NavType.IntType }),
            ) { back ->
                val args = back.arguments
                entry(args?.getInt("i") ?: 0)?.let { e ->
                    EpisodeScreen(vm, e, args?.getInt("n") ?: 1, onBack = nav::popBackStack)
                }
            }
            composable("add") {
                AddScreen(
                    vm = vm,
                    initialUrl = shared.orEmpty(),
                    onDone = {
                        sharedUrl.value = null
                        nav.popBackStack()
                    },
                    onSettings = { nav.navigate("settings") },
                )
            }
            composable("settings") {
                SettingsScreen(vm, onBack = nav::popBackStack)
            }
        }
    }
}
