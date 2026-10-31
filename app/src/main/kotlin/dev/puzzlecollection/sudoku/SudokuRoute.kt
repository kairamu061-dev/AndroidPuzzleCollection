package dev.puzzlecollection.sudoku

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.puzzlecollection.R
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.session.TimedGameRoute
import dev.puzzlecollection.ui.formatElapsed

const val SUDOKU_SAVE_PATH = "sudoku/save.txt"

@Composable
fun SudokuRoute(onExit: () -> Unit, viewModel: SudokuViewModel = sudokuViewModel()) {
    TimedGameRoute(
        viewModel = viewModel,
        title = stringResource(R.string.sudoku_title),
        options = Difficulty.entries.map { it to difficultyLabel(it) },
        resumeLabel = { start ->
            start.saved?.let { saved ->
                stringResource(
                    R.string.resume_game,
                    difficultyLabel(saved.puzzle.difficulty),
                    formatElapsed(start.elapsedMillis),
                )
            }
        },
        onExit = onExit,
    ) { playing ->
        SudokuGameScreen(
            state = playing,
            viewModel = viewModel,
            onExit = {
                viewModel.leaveGame()
                onExit()
            },
        )
    }
}

@Composable
private fun sudokuViewModel(): SudokuViewModel {
    val appContext = LocalContext.current.applicationContext
    return viewModel(
        factory = viewModelFactory {
            initializer { SudokuViewModel(SaveFile(appContext.filesDir, SUDOKU_SAVE_PATH)) }
        },
    )
}

@Composable
internal fun difficultyLabel(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    },
)
