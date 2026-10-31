package dev.puzzlecollection.nonogram

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.onAllNodesWithTextExists
import dev.puzzlecollection.ui.PuzzleTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

private val HEART = NonogramPuzzle(
    5,
    (".#.#." + "#####" + "#####" + ".###." + "..#..").map { it == '#' },
)

class NonogramLayoutTest {
    private val layout = NonogramLayout(5, 2, 2, 700f, 700f)

    @Test fun `U-01 points map to cells`() {
        assertEquals(0, layout.cellAt(layout.cellCenterX(0), layout.cellCenterY(0)))
        assertEquals(13, layout.cellAt(layout.cellCenterX(13), layout.cellCenterY(13)))
        assertNull(layout.cellAt(1f, 1f))
        assertNull(layout.cellAt(699.9f, 1f))
    }
}

@RunWith(AndroidJUnit4::class)
class NonogramScreenTest {
    @get:Rule val folder = TemporaryFolder()
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val store by lazy { SaveFile(folder.root, NONOGRAM_SAVE_PATH) }
    private val file get() = File(folder.root, NONOGRAM_SAVE_PATH)

    private fun launch(saved: NonogramState? = null) {
        saved?.let { store.write(NonogramSaveCodec.encode(it, 1_000)) }
        val viewModel = NonogramViewModel(store, clock = { 0L }, generate = { _, _ -> HEART })
        rule.setContent { PuzzleTheme { NonogramRoute(onExit = {}, viewModel = viewModel) } }
        rule.waitUntil { rule.onAllNodesWithTextExists("新しいゲーム") }
    }

    private fun play(saved: NonogramState = NonogramState(HEART)) {
        launch(saved)
        rule.onNodeWithText("続きから", substring = true).performClick()
        rule.waitUntil { rule.onAllNodesWithTextExists("塗る") }
    }

    private val board get() = rule.onNodeWithTag("nonogram-board")

    private fun description(): String =
        board.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()

    private fun tap(cell: Int) {
        val size = board.fetchSemanticsNode().size
        val puzzle = HEART
        val layout = NonogramLayout(
            5,
            puzzle.rowClues.maxOf { it.size },
            puzzle.columnClues.maxOf { it.size },
            size.width.toFloat(),
            size.height.toFloat(),
        )
        board.performTouchInput { click(Offset(layout.cellCenterX(cell), layout.cellCenterY(cell))) }
        rule.waitForIdle()
    }

    @Test fun `I-01 start screen`() {
        launch()
        rule.onNodeWithText("20×20").assertIsDisplayed()
        assertFalse(rule.onAllNodesWithTextExists("15×15"))
    }

    @Test fun `I-02 a tap fills`() {
        play()
        tap(1)
        assertTrue(description(), "塗り 1 個" in description())
    }

    @Test fun `I-03 the cross tool marks without filling`() {
        play()
        rule.onNodeWithText("×").performClick()
        tap(0)
        assertTrue(description(), "塗り 0 個" in description())
        assertTrue(description(), "× 1 個" in description())
    }

    @Test fun `I-04 undo and redo`() {
        play()
        tap(1)
        rule.onNodeWithText("戻す").performClick()
        assertTrue("塗り 0 個" in description())
        rule.onNodeWithText("進む").performClick()
        assertTrue("塗り 1 個" in description())
    }

    @Test fun `I-05 the last correct cell clears, a wrong one does not`() {
        val last = HEART.solution.lastIndexOf(true)
        val cells = HEART.solution.mapIndexed { i, filled ->
            if (filled && i != last) Mark.FILLED else Mark.EMPTY
        }
        play(NonogramState(HEART, cells))
        tap(0)
        assertFalse(rule.onAllNodesWithTextExists("クリア"))
        tap(0)
        tap(last)
        rule.onNodeWithText("クリア").assertIsDisplayed()
        rule.waitUntil { !file.exists() }
    }

    @Test fun `I-06 resume restores the board`() {
        play(NonogramState(HEART, HEART.solution.indices.map { if (it == 1) Mark.FILLED else Mark.EMPTY }))
        assertTrue(description(), "塗り 1 個" in description())
    }
}
