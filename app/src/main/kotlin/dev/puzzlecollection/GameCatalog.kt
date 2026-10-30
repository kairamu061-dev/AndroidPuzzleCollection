package dev.puzzlecollection

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import dev.puzzlecollection.sudoku.SudokuRoute

/** [id] restores the open game after process death; renaming it strands that state. */
data class GameEntry(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val content: @Composable (onExit: () -> Unit) -> Unit,
)

object GameCatalog {
    val games: List<GameEntry> = listOf(
        GameEntry(
            id = "sudoku",
            titleRes = R.string.sudoku_title,
            descriptionRes = R.string.sudoku_description,
            content = { onExit -> SudokuRoute(onExit) },
        ),
    )
}
