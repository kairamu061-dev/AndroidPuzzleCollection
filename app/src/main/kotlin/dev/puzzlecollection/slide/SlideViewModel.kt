package dev.puzzlecollection.slide

import android.os.SystemClock
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.session.TimedGameViewModel
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class SlideViewModel(
    saveFile: SaveFile,
    clock: () -> Long = SystemClock::elapsedRealtime,
    private val generate: (Int) -> SlideState = { size -> SlideGenerator.generate(size, Random.Default) },
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TimedGameViewModel<SlideState, Int>(saveFile, clock, ioDispatcher, computeDispatcher) {
    init {
        loadSavedGame()
    }

    fun tap(cell: Int) = update { it.tap(cell) }

    override fun create(option: Int, onAttempt: () -> Unit) = generate(option)
    override fun encode(game: SlideState, elapsedMillis: Long) = SlideSaveCodec.encode(game, elapsedMillis)
    override fun decode(text: String) = SlideSaveCodec.decode(text)
    override fun isFinished(game: SlideState) = game.isSolved
    override fun needsSave(before: SlideState, after: SlideState) = before != after
}
