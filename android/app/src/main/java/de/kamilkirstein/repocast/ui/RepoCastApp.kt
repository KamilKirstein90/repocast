package de.kamilkirstein.repocast.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.kamilkirstein.repocast.data.PodcastEntry
import kotlinx.coroutines.flow.MutableStateFlow

/** Navigation between the screens, with the mini player docked at the bottom. */
@Composable
fun RepoCastApp(sharedUrl: MutableStateFlow<String?>) {
    val vm: AppViewModel = viewModel()
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
