package dev.puzzlecollection.numberlink

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
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

/** Same board as the core tests' SAMPLE: three lines on a 4x4. */
private val BOARD = NumberlinkPuzzle(4, listOf(listOf(0, 1, 2, 3, 7), listOf(4, 5, 6, 10, 11, 15, 14), listOf(8, 12, 13, 9)))

class NumberlinkLayoutTest {
    private val layout = NumberlinkLayout(4, 400f, 600f)

    @Test fun `U-01 points map to cells and the grid is centred`() {
        assertEquals(100f, layout.top)
        assertEquals(5, layout.cellAt(layout.centerX(5), layout.centerY(5)))
        assertNull(layout.cellAt(10f, 10f))
    }

    @Test fun `U-02 a long jump walks every cell in between`() {
        val cells = layout.cellsAlong(layout.centerX(0), layout.centerY(0), layout.centerX(3), layout.centerY(3))
        assertEquals(listOf(1, 2, 3), cells)
    }
}

@RunWith(AndroidJUnit4::class)
class NumberlinkScreenTest {
    @get:Rule val folder = TemporaryFolder()
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val store by lazy { SaveFile(folder.root, NUMBERLINK_SAVE_PATH) }
    private val file get() = File(folder.root, NUMBERLINK_SAVE_PATH)

    private fun launch(saved: NumberlinkState? = null) {
        saved?.let { store.write(NumberlinkSaveCodec.encode(it, 1_000)) }
        val viewModel = NumberlinkViewModel(store, clock = { 0L }, generate = { _, _ -> BOARD })
        rule.setContent { PuzzleTheme { NumberlinkRoute(onExit = {}, viewModel = viewModel) } }
        rule.waitUntil { rule.onAllNodesWithTextExists("新しいゲーム") }
    }

    private fun play(saved: NumberlinkState = NumberlinkState(BOARD)) {
        launch(saved)
        rule.onNodeWithText("続きから", substring = true).performClick()
        rule.waitUntil { rule.onAllNodesWithTextExists("戻す") }
    }

    private val board get() = rule.onNodeWithTag("numberlink-board")

    private fun description(): String =
        board.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()

    private fun layout(): NumberlinkLayout {
        val size = board.fetchSemanticsNode().size
        return NumberlinkLayout(4, size.width.toFloat(), size.height.toFloat())
    }

    private fun drag(vararg cells: Int) {
        val layout = layout()
        board.performTouchInput {
            down(Offset(layout.centerX(cells[0]), layout.centerY(cells[0])))
            cells.drop(1).forEach { moveTo(Offset(layout.centerX(it), layout.centerY(it))) }
            up()
        }
        rule.waitForIdle()
    }

    @Test fun `I-01 start screen`() {
        launch()
        listOf("6×6", "7×7", "8×8").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test fun `I-02 dragging from an endpoint draws a line`() {
        play()
        assertTrue(description(), "空きマス 10" in description())
        drag(0, 1, 2)
        assertTrue(description(), "空きマス 8" in description())
    }

    @Test fun `I-02 one fast move draws the same line as a slow one`() {
        play()
        drag(0, 3)
        val fast = description()
        rule.onNodeWithText("戻す").performClick()
        drag(0, 1, 2, 3)
        assertTrue(fast, "空きマス 7" in fast)
        assertEquals(fast, description())
    }

    @Test fun `I-03 undo and redo`() {
        play()
        drag(0, 1, 2)
        rule.onNodeWithText("戻す").performClick()
        assertTrue("空きマス 10" in description())
        rule.onNodeWithText("進む").performClick()
        assertTrue("空きマス 8" in description())
    }

    @Test fun `I-04 joined lines with empty cells say so`() {
        play(NumberlinkState(BOARD, listOf(listOf(0, 1, 2, 3, 7), listOf(4, 5, 6, 10, 14), listOf(8, 12, 13, 9))))
        rule.onNodeWithText("空きマスが 2 個あります").assertIsDisplayed()
    }

    @Test fun `I-04 the message waits until every pair is joined`() {
        play()
        assertFalse(rule.onAllNodesWithTextExists("空きマスが"))
        rule.onNodeWithText("つないだ線 0 / 3").assertIsDisplayed()
    }

    @Test fun `I-05 the last join clears and removes the save`() {
        play(NumberlinkState(BOARD, listOf(listOf(0, 1, 2, 3, 7), listOf(4, 5, 6, 10, 11, 15, 14), listOf(8, 12, 13))))
        drag(13, 9)
        rule.onNodeWithText("クリア").assertIsDisplayed()
        rule.waitUntil { !file.exists() }
    }

    @Test fun `I-06 resume restores the lines`() {
        play(NumberlinkState(BOARD, listOf(listOf(0, 1), emptyList(), emptyList())))
        assertTrue(description(), "空きマス 9" in description())
    }
}
