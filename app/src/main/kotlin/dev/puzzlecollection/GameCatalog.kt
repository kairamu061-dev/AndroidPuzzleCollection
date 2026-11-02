package dev.puzzlecollection

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import dev.puzzlecollection.game2048.Game2048Route
import dev.puzzlecollection.nonogram.NonogramRoute
import dev.puzzlecollection.numberlink.NumberlinkRoute
import dev.puzzlecollection.slide.SlideRoute
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
        GameEntry(
            id = "slide",
            titleRes = R.string.slide_title,
            descriptionRes = R.string.slide_description,
            content = { onExit -> SlideRoute(onExit) },
        ),
        GameEntry(
            id = "game2048",
            titleRes = R.string.game2048_title,
            descriptionRes = R.string.game2048_description,
            content = { onExit -> Game2048Route(onExit) },
        ),
        GameEntry(
            id = "nonogram",
            titleRes = R.string.nonogram_title,
            descriptionRes = R.string.nonogram_description,
            content = { onExit -> NonogramRoute(onExit) },
        ),
        GameEntry(
            id = "numberlink",
            titleRes = R.string.numberlink_title,
            descriptionRes = R.string.numberlink_description,
            content = { onExit -> NumberlinkRoute(onExit) },
        ),
    )
}
