package dev.puzzlecollection.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.puzzlecollection.common.PlayTimer
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.data.SaveWriter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface TimedUiState<out G> {
    data object Loading : TimedUiState<Nothing>
    data class Start<G>(val saved: G?, val elapsedMillis: Long) : TimedUiState<G>
    data object Generating : TimedUiState<Nothing>
    data class Playing<G>(val game: G, val elapsedMillis: Long) : TimedUiState<G>
}

/** A game with a start screen, a play timer, and one autosaved session. [O] is the start option. */
abstract class TimedGameViewModel<G : Any, O>(
    private val saveFile: SaveFile,
    private val clock: () -> Long,
    private val ioDispatcher: CoroutineDispatcher,
    private val computeDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _state = MutableStateFlow<TimedUiState<G>>(TimedUiState.Loading)
    val state: StateFlow<TimedUiState<G>> = _state.asStateFlow()

    private val saves = SaveWriter(viewModelScope, saveFile, ioDispatcher)
    private var game: G? = null
    private var timer = PlayTimer()
    private var ticker: Job? = null
    private var generation: Job? = null

    protected abstract fun create(option: O, onAttempt: () -> Unit): G
    protected abstract fun encode(game: G, elapsedMillis: Long): String
    protected abstract fun decode(text: String): Pair<G, Long>?
    protected abstract fun isFinished(game: G): Boolean
    protected abstract fun needsSave(before: G, after: G): Boolean

    /** Subclasses call this last in `init`: it reaches the abstract members above. */
    protected fun loadSavedGame() {
        viewModelScope.launch {
            val saved = withContext(ioDispatcher) { saveFile.read()?.let(::decode) }
            if (saved != null) {
                game = saved.first
                timer = PlayTimer(saved.second)
            }
            showStart()
        }
    }

    fun resumeGame() {
        if (game != null) publishPlaying()
    }

    fun startNewGame(option: O) {
        _state.value = TimedUiState.Generating
        generation = viewModelScope.launch {
            val fresh = withContext(computeDispatcher) {
                create(option) { ensureActive() }
            }
            game = fresh
            timer = PlayTimer()
            save(fresh)
            publishPlaying()
        }
    }

    fun cancelGeneration() {
        generation?.cancel()
        showStart()
    }

    /** Leaves the board for the start screen. A finished game ends here. */
    fun leaveGame() {
        pauseTimer()
        game?.let { if (isFinished(it)) game = null else save(it) }
        showStart()
    }

    fun onScreenVisible() {
        if (game?.let(::isFinished) != false) return
        timer.start(clock())
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (true) {
                publishPlaying()
                delay(TICK_MILLIS)
            }
        }
    }

    fun onScreenHidden() {
        pauseTimer()
        game?.takeUnless(::isFinished)?.let(::save)
    }

    protected fun update(operation: (G) -> G) {
        val before = game ?: return
        val after = operation(before)
        if (after === before) return
        game = after
        if (isFinished(after)) {
            pauseTimer()
            saves.delete()
        } else if (needsSave(before, after)) {
            save(after)
        }
        publishPlaying()
    }

    private fun pauseTimer() {
        ticker?.cancel()
        timer.pause(clock())
    }

    private fun save(state: G) {
        saves.write(encode(state, timer.elapsed(clock())))
    }

    private fun publishPlaying() {
        val current = game ?: return
        _state.value = TimedUiState.Playing(current, timer.elapsed(clock()))
    }

    private fun showStart() {
        _state.value = TimedUiState.Start(game, timer.elapsed(clock()))
    }

    private companion object {
        const val TICK_MILLIS = 500L
    }
}
