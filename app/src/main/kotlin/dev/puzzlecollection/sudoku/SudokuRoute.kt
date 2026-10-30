package dev.puzzlecollection.sudoku

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.puzzlecollection.R

@Composable
fun SudokuRoute(onExit: () -> Unit, viewModel: SudokuViewModel = sudokuViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        SudokuUiState.Loading -> Progress()
        is SudokuUiState.Start -> {
            BackHandler(onBack = onExit)
            SudokuStartScreen(
                saved = current.saved,
                onResume = viewModel::resumeGame,
                onNewGame = viewModel::startNewGame,
                onBack = onExit,
            )
        }
        SudokuUiState.Generating -> {
            BackHandler(onBack = viewModel::cancelGeneration)
            Progress()
        }
        is SudokuUiState.Playing -> {
            BackHandler(onBack = viewModel::leaveGame)
            LifecycleResumeEffect(viewModel) {
                viewModel.onScreenVisible()
                onPauseOrDispose { viewModel.onScreenHidden() }
            }
            SudokuGameScreen(
                state = current,
                viewModel = viewModel,
                onExit = {
                    viewModel.leaveGame()
                    onExit()
                },
            )
        }
    }
}

@Composable
private fun sudokuViewModel(): SudokuViewModel {
    val appContext = LocalContext.current.applicationContext
    return viewModel(
        factory = viewModelFactory {
            initializer { SudokuViewModel(SaveRepository(appContext.filesDir)) }
        },
    )
}

@Composable
private fun Progress() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.testTag("progress"))
        }
    }
}

@Composable
internal fun difficultyLabel(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    },
)
