package dev.puzzlecollection.nonogram

import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.data.SaveWriter
import dev.puzzlecollection.session.TimedGameViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NonogramCatalog(
    val pictures: List<NonogramPicture> = emptyList(),
    val completed: Set<String> = emptySet(),
)

class NonogramViewModel(
    saveFile: SaveFile,
    private val progressFile: SaveFile,
    clock: () -> Long = SystemClock::elapsedRealtime,
    private val loadPictures: () -> List<NonogramPicture> = { NonogramPictures.all },
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TimedGameViewModel<NonogramState, NonogramPicture>(saveFile, clock, ioDispatcher, computeDispatcher) {
    private val _catalog = MutableStateFlow(NonogramCatalog())
    val catalog: StateFlow<NonogramCatalog> = _catalog.asStateFlow()

    private val progressWrites = SaveWriter(viewModelScope, progressFile, ioDispatcher)

    init {
        loadSavedGame()
    }

    fun strokeStart(cell: Int) = update { it.strokeStart(cell) }
    fun strokeTo(cell: Int) = update { it.strokeTo(cell) }
    fun strokeEnd() = update { it.strokeEnd() }
    fun strokeCancel() = update { it.strokeCancel() }
    fun selectTool(tool: Mark) = update { it.selectTool(tool) }
    fun undo() = update { it.undo() }
    fun redo() = update { it.redo() }

    override fun create(option: NonogramPicture, onAttempt: () -> Unit) =
        NonogramState(option.puzzle, option.id)

    override fun encode(game: NonogramState, elapsedMillis: Long) = NonogramSaveCodec.encode(game, elapsedMillis)
    override fun decode(text: String) = NonogramSaveCodec.decode(text)
    override fun isFinished(game: NonogramState) = game.isSolved
    override fun needsSave(before: NonogramState, after: NonogramState) = before.cells != after.cells

    override fun loadExtra() {
        val completed = progressFile.read()?.let(NonogramProgress::decode).orEmpty()
        _catalog.value = NonogramCatalog(loadPictures(), completed)
    }

    override fun onFinished(game: NonogramState) {
        val id = game.pictureId ?: return
        val completed = _catalog.value.completed + id
        _catalog.value = _catalog.value.copy(completed = completed)
        progressWrites.write(NonogramProgress.encode(completed))
    }
}
