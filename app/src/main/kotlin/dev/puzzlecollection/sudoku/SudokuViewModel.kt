package dev.puzzlecollection.sudoku

import android.os.SystemClock
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.session.TimedGameViewModel
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class SudokuViewModel(
    saveFile: SaveFile,
    clock: () -> Long = SystemClock::elapsedRealtime,
    private val generate: (Difficulty, () -> Unit) -> Puzzle = { difficulty, onAttempt ->
        Generator.generate(difficulty, Random.Default, onAttempt).puzzle
    },
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TimedGameViewModel<GameState, Difficulty>(saveFile, clock, ioDispatcher, computeDispatcher) {
    init {
        loadSavedGame()
    }

    fun selectCell(cell: Int) = update { it.select(cell) }
    fun inputDigit(digit: Int) = update { it.input(digit) }
    fun erase() = update { it.erase() }
    fun undo() = update { it.undo() }
    fun redo() = update { it.redo() }
    fun toggleMemoMode() = update { it.toggleMemoMode() }

    override fun create(option: Difficulty, onAttempt: () -> Unit) =
        GameState(generate(option, onAttempt))

    override fun encode(game: GameState, elapsedMillis: Long) =
        SaveCodec.encode(SavedGame(game, elapsedMillis))

    override fun decode(text: String) =
        SaveCodec.decode(text)?.let { it.state to it.elapsedMillis }

    override fun isFinished(game: GameState) = game.isSolved

    override fun needsSave(before: GameState, after: GameState) = shouldSave(before, after)
}
