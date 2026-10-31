package dev.puzzlecollection.session

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.puzzlecollection.ui.GameStartScreen
import dev.puzzlecollection.ui.LoadingScreen

/** [options] pairs each start option with its label. */
@Composable
fun <G : Any, O> TimedGameRoute(
    viewModel: TimedGameViewModel<G, O>,
    title: String,
    options: List<Pair<O, String>>,
    resumeLabel: @Composable (TimedUiState.Start<G>) -> String?,
    onExit: () -> Unit,
    playing: @Composable (TimedUiState.Playing<G>) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        TimedUiState.Loading -> LoadingScreen()
        is TimedUiState.Start -> {
            BackHandler(onBack = onExit)
            GameStartScreen(
                title = title,
                resumeLabel = resumeLabel(current),
                optionLabels = options.map { it.second },
                onResume = viewModel::resumeGame,
                onOption = { index -> viewModel.startNewGame(options[index].first) },
                onBack = onExit,
            )
        }
        TimedUiState.Generating -> {
            BackHandler(onBack = viewModel::cancelGeneration)
            LoadingScreen()
        }
        is TimedUiState.Playing -> {
            BackHandler(onBack = viewModel::leaveGame)
            LifecycleResumeEffect(viewModel) {
                viewModel.onScreenVisible()
                onPauseOrDispose { viewModel.onScreenHidden() }
            }
            playing(current)
        }
    }
}
