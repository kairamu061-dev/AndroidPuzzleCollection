package dev.puzzlecollection.sudoku

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SaveRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val repository by lazy { SaveRepository(folder.root) }
    private val game = SavedGame(GameState(PUZZLE).select(EMPTY_CELL).input(4), 42)

    @Test fun `I-01 write then read`() {
        assertNull(repository.read())
        repository.write(game)
        val read = repository.read()!!
        assertEquals(game.state.board, read.state.board)
        assertEquals(42, read.elapsedMillis)
    }

    @Test fun `I-02 a damaged file reads as no save`() {
        File(folder.root, "sudoku").mkdirs()
        File(folder.root, "sudoku/save.txt").writeText("garbage")
        assertNull(repository.read())
    }

    @Test fun `I-03 delete removes the save`() {
        repository.write(game)
        repository.delete()
        assertNull(repository.read())
    }
}
