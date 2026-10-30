package dev.puzzlecollection.sudoku

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SavedSummary(val difficulty: Difficulty, val elapsedMillis: Long)

sealed interface SudokuUiState {
    data object Loading : SudokuUiState
    data class Start(val saved: SavedSummary?) : SudokuUiState
    data object Generating : SudokuUiState
    data class Playing(val game: GameState, val elapsedMillis: Long) : SudokuUiState
}

class SudokuViewModel(
    private val repository: SaveRepository,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val generate: (Difficulty, () -> Unit) -> Puzzle = { difficulty, onAttempt ->
        Generator.generate(difficulty, Random.Default, onAttempt).puzzle
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private sealed interface SaveCommand {
        data class Write(val game: SavedGame) : SaveCommand
        data object Delete : SaveCommand
    }

    private val _state = MutableStateFlow<SudokuUiState>(SudokuUiState.Loading)
    val state: StateFlow<SudokuUiState> = _state.asStateFlow()

    private var game: GameState? = null
    private var timer = PlayTimer()
    private var ticker: Job? = null
    private var generation: Job? = null

    // One consumer, latest wins: concurrent writers could land an older state last.
    private val saves = Channel<SaveCommand>(Channel.CONFLATED)

    init {
        viewModelScope.launch(ioDispatcher) {
            for (command in saves) {
                when (command) {
                    is SaveCommand.Write -> repository.write(command.game)
                    SaveCommand.Delete -> repository.delete()
                }
            }
        }
        viewModelScope.launch {
            val saved = withContext(ioDispatcher) { repository.read() }
            if (saved != null) {
                game = saved.state
                timer = PlayTimer(saved.elapsedMillis)
            }
            showStart()
        }
    }

    fun resumeGame() {
        if (game != null) publishPlaying()
    }

    fun startNewGame(difficulty: Difficulty) {
        _state.value = SudokuUiState.Generating
        generation = viewModelScope.launch {
            val puzzle = withContext(computeDispatcher) {
                generate(difficulty) { ensureActive() }
            }
            val fresh = GameState(puzzle)
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

    /** Leaves the board for the start screen. A solved game ends here. */
    fun leaveGame() {
        pauseTimer()
        if (game?.isSolved == true) game = null else game?.let(::save)
        showStart()
    }

    fun selectCell(cell: Int) = update { it.select(cell) }
    fun inputDigit(digit: Int) = update { it.input(digit) }
    fun erase() = update { it.erase() }
    fun undo() = update { it.undo() }
    fun redo() = update { it.redo() }
    fun toggleMemoMode() = update { it.toggleMemoMode() }

    fun onScreenVisible() {
        if (game?.isSolved != false) return
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
        game?.takeUnless { it.isSolved }?.let(::save)
    }

    private fun update(operation: (GameState) -> GameState) {
        val before = game ?: return
        val after = operation(before)
        if (after === before) return
        game = after
        if (after.isSolved) {
            pauseTimer()
            saves.trySend(SaveCommand.Delete)
        } else if (shouldSave(before, after)) {
            save(after)
        }
        publishPlaying()
    }

    private fun pauseTimer() {
        ticker?.cancel()
        timer.pause(clock())
    }

    private fun save(state: GameState) {
        saves.trySend(SaveCommand.Write(SavedGame(state, timer.elapsed(clock()))))
    }

    private fun publishPlaying() {
        val current = game ?: return
        _state.value = SudokuUiState.Playing(current, timer.elapsed(clock()))
    }

    private fun showStart() {
        val summary = game?.let { SavedSummary(it.puzzle.difficulty, timer.elapsed(clock())) }
        _state.value = SudokuUiState.Start(summary)
    }

    private companion object {
        const val TICK_MILLIS = 500L
    }
}
