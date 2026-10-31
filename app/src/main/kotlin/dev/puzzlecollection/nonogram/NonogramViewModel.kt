package dev.puzzlecollection.nonogram

import android.os.SystemClock
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.session.TimedGameViewModel
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class NonogramViewModel(
    saveFile: SaveFile,
    clock: () -> Long = SystemClock::elapsedRealtime,
    private val generate: (Int, () -> Unit) -> NonogramPuzzle = { size, onAttempt ->
        NonogramGenerator.generate(size, Random.Default, onAttempt)
    },
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TimedGameViewModel<NonogramState, Int>(saveFile, clock, ioDispatcher, computeDispatcher) {
    init {
        loadSavedGame()
    }

    fun strokeStart(cell: Int) = update { it.strokeStart(cell) }
    fun strokeTo(cell: Int) = update { it.strokeTo(cell) }
    fun strokeEnd() = update { it.strokeEnd() }
    fun selectTool(tool: Mark) = update { it.selectTool(tool) }
    fun undo() = update { it.undo() }
    fun redo() = update { it.redo() }

    override fun create(option: Int, onAttempt: () -> Unit) = NonogramState(generate(option, onAttempt))
    override fun encode(game: NonogramState, elapsedMillis: Long) = NonogramSaveCodec.encode(game, elapsedMillis)
    override fun decode(text: String) = NonogramSaveCodec.decode(text)
    override fun isFinished(game: NonogramState) = game.isSolved
    override fun needsSave(before: NonogramState, after: NonogramState) = before.cells != after.cells
}
