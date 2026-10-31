package dev.puzzlecollection.game2048

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.data.SaveWriter
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [game] is null until the save has been read. */
data class Game2048UiState(val game: Game2048State? = null, val showReached: Boolean = false)

class Game2048ViewModel(
    private val saveFile: SaveFile,
    private val random: Random = Random.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _state = MutableStateFlow(Game2048UiState())
    val state: StateFlow<Game2048UiState> = _state.asStateFlow()

    private val saves = SaveWriter(viewModelScope, saveFile, ioDispatcher)

    init {
        viewModelScope.launch {
            val saved = withContext(ioDispatcher) { saveFile.read()?.let(Game2048SaveCodec::decode) }
            if (saved != null) publish(saved) else startNewGame()
        }
    }

    fun move(direction: Direction) {
        val before = _state.value.game ?: return
        val after = before.move(direction, random)
        if (after === before) return
        // A finished board stays saved: it is the only record of the best score.
        saves.write(Game2048SaveCodec.encode(after))
        _state.value = Game2048UiState(after, showReached = after.reached2048 && !before.reached2048)
    }

    fun dismissReached() {
        _state.value = _state.value.copy(showReached = false)
    }

    fun startNewGame() {
        val fresh = Game2048State.newGame(_state.value.game?.best ?: 0, random)
        saves.write(Game2048SaveCodec.encode(fresh))
        publish(fresh)
    }

    private fun publish(game: Game2048State) {
        _state.value = Game2048UiState(game)
    }
}
