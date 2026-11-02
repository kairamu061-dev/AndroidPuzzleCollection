package dev.puzzlecollection.numberlink

import android.os.SystemClock
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.session.TimedGameViewModel
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class NumberlinkViewModel(
    saveFile: SaveFile,
    clock: () -> Long = SystemClock::elapsedRealtime,
    private val generate: (Int, () -> Unit) -> NumberlinkPuzzle = { size, onAttempt ->
        NumberlinkGenerator.generate(size, Random.Default, onAttempt).puzzle
    },
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TimedGameViewModel<NumberlinkState, Int>(saveFile, clock, ioDispatcher, computeDispatcher) {
    init {
        loadSavedGame()
    }

    fun strokeStart(cell: Int) = update { it.strokeStart(cell) }
    fun strokeTo(cell: Int) = update { it.strokeTo(cell) }
    fun strokeEnd() = update { it.strokeEnd() }
    fun undo() = update { it.undo() }
    fun redo() = update { it.redo() }

    override fun create(option: Int, onAttempt: () -> Unit) = NumberlinkState(generate(option, onAttempt))
    override fun encode(game: NumberlinkState, elapsedMillis: Long) = NumberlinkSaveCodec.encode(game, elapsedMillis)
    override fun decode(text: String) = NumberlinkSaveCodec.decode(text)
    override fun isFinished(game: NumberlinkState) = game.isSolved
    override fun needsSave(before: NumberlinkState, after: NumberlinkState) = before.paths != after.paths
}
