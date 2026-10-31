package dev.puzzlecollection.nonogram

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
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

private val PICTURES = listOf(
    NonogramPicture("heart", "ハート", HEART),
    NonogramPicture("plus", "たす", NonogramPuzzle(5, ("..#.." + "..#.." + "#####" + "..#.." + "..#..").map { it == '#' })),
)

class NonogramLayoutTest {
    private val layout = NonogramLayout(5, 2, 2, 700f, 700f)

    @Test fun `U-02 zoom keeps the point under the fingers`() {
        val x = layout.cellCenterX(13)
        val y = layout.cellCenterY(13)
        val zoomed = NonogramViewport().zoomedBy(2f, x, y, layout)
        assertEquals(2f, zoomed.scale)
        assertEquals(13, layout.cellAt(x, y, zoomed))
    }

    @Test fun `U-02 zoom and pan stay inside the grid`() {
        val zoomed = NonogramViewport().zoomedBy(100f, layout.gridLeft, layout.gridTop, layout)
        assertEquals(NonogramViewport.MAX_SCALE, zoomed.scale)
        assertEquals(0f, zoomed.pannedBy(500f, 500f, layout).panX)
        val limit = layout.gridSize * (zoomed.scale - 1f)
        assertEquals(limit, zoomed.pannedBy(-99_999f, 0f, layout).panX)
        assertEquals(1f, zoomed.zoomedBy(0.001f, 0f, 0f, layout).scale)
    }

    @Test fun `U-02 clues are not cells while zoomed`() {
        val zoomed = NonogramViewport().zoomedBy(2f, layout.gridLeft, layout.gridTop, layout)
        assertNull(layout.cellAt(layout.gridLeft - 1f, layout.gridTop + 1f, zoomed))
    }

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
    private val progress by lazy { SaveFile(folder.root, NONOGRAM_PROGRESS_PATH) }
    private val file get() = File(folder.root, NONOGRAM_SAVE_PATH)
    private val progressFile get() = File(folder.root, NONOGRAM_PROGRESS_PATH)

    private fun launch(saved: NonogramState? = null, completed: Set<String> = emptySet()) {
        saved?.let { store.write(NonogramSaveCodec.encode(it, 1_000)) }
        if (completed.isNotEmpty()) progress.write(NonogramProgress.encode(completed))
        val viewModel = NonogramViewModel(store, progress, clock = { 0L }, loadPictures = { PICTURES })
        rule.setContent { PuzzleTheme { NonogramRoute(onExit = {}, viewModel = viewModel) } }
        rule.waitUntil { rule.onAllNodesWithTextExists("新しいゲーム") }
    }

    private fun play(saved: NonogramState = NonogramState(HEART, "heart")) {
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
        rule.onNodeWithText("次の絵（問題 1）").assertIsDisplayed()
        rule.onNodeWithText("完成した絵 0 / 2").assertIsDisplayed()
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
        play(NonogramState(HEART, "heart", cells))
        tap(0)
        assertFalse(rule.onAllNodesWithTextExists("クリア"))
        tap(0)
        tap(last)
        rule.onNodeWithText("クリア").assertIsDisplayed()
        rule.onNodeWithText("ハート", substring = true).assertIsDisplayed()
        rule.waitUntil { !file.exists() }
        rule.waitUntil { progressFile.exists() }
        assertEquals(setOf("heart"), NonogramProgress.decode(progressFile.readText()))
    }

    @Test fun `I-07 a pinch zooms and show-all resets`() {
        play()
        rule.onNodeWithText("全体").assertIsNotEnabled()
        board.performTouchInput {
            pinch(center - Offset(10f, 0f), center - Offset(200f, 0f), center + Offset(10f, 0f), center + Offset(200f, 0f))
        }
        rule.waitForIdle()
        rule.onNodeWithText("全体").assertIsEnabled()
        rule.onNodeWithText("全体").performClick()
        rule.onNodeWithText("全体").assertIsNotEnabled()
    }

    @Test fun `I-08 a second finger cancels the first finger's mark`() {
        play()
        val size = board.fetchSemanticsNode().size
        val layout = NonogramLayout(
            5,
            HEART.rowClues.maxOf { it.size },
            HEART.columnClues.maxOf { it.size },
            size.width.toFloat(),
            size.height.toFloat(),
        )
        val first = Offset(layout.cellCenterX(1), layout.cellCenterY(1))
        board.performTouchInput { down(0, first) }
        rule.waitForIdle()
        assertTrue(description(), "塗り 1 個" in description())
        board.performTouchInput { down(1, center) }
        rule.waitForIdle()
        board.performTouchInput {
            moveBy(1, Offset(40f, 0f))
            up(1)
            up(0)
        }
        rule.waitForIdle()
        assertTrue(description(), "塗り 0 個" in description())
    }

    @Test fun `I-09 a finished picture can be replayed, an unfinished one cannot`() {
        launch(completed = setOf("heart"))
        rule.onNodeWithText("次の絵（問題 2）").assertIsDisplayed()
        rule.onNodeWithText("？").assertHasNoClickAction()
        rule.onNodeWithContentDescription("ハート").performClick()
        rule.waitUntil { rule.onAllNodesWithTextExists("問題 1") }
        rule.onNodeWithText("問題 1").assertIsDisplayed()
    }

    @Test fun `I-10 nothing left to start once all are finished`() {
        launch(completed = setOf("heart", "plus"))
        assertFalse(rule.onAllNodesWithTextExists("次の絵"))
        rule.onNodeWithText("完成した絵 2 / 2").assertIsDisplayed()
    }

    @Test fun `I-06 resume restores the board`() {
        play(NonogramState(HEART, "heart", HEART.solution.indices.map { if (it == 1) Mark.FILLED else Mark.EMPTY }))
        assertTrue(description(), "塗り 1 個" in description())
    }
}
