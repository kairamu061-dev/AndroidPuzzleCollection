package dev.puzzlecollection.sudoku

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.onAllNodesWithTextExists
import java.io.File
import dev.puzzlecollection.ui.PuzzleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SudokuScreenTest {
    @get:Rule val folder = TemporaryFolder()
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val store by lazy { SaveFile(folder.root, SUDOKU_SAVE_PATH) }

    private val saveFile get() = File(folder.root, "sudoku/save.txt")

    /** Reads the file directly: SaveFile.read must not run alongside the view model's writes. */
    private fun savedValue(cell: Int): Int? =
        runCatching { SaveCodec.decode(saveFile.readText()) }.getOrNull()?.state?.board?.values?.get(cell)

    private fun launch(saved: GameState? = null) {
        saved?.let { store.write(SaveCodec.encode(SavedGame(it, 5_000))) }
        val viewModel = SudokuViewModel(store, clock = { 0L }, generate = { _, _ -> PUZZLE })
        rule.setContent { PuzzleTheme { SudokuRoute(onExit = {}, viewModel = viewModel) } }
        rule.waitUntil { rule.onAllNodesWithTextExists("新しいゲーム") }
    }

    private fun launchPlaying() {
        launch()
        rule.onNodeWithText("ふつう").performClick()
        waitForBoard()
    }

    private fun launchResumed(saved: GameState) {
        launch(saved)
        rule.onNodeWithText("続きから", substring = true).performClick()
        waitForBoard()
    }

    private fun waitForBoard() = rule.waitUntil(5_000) {
        rule.onAllNodesWithTextExists("メモ")
    }

    private fun cell(index: Int) = rule.onNodeWithTag("cell-$index")
    private fun digit(value: Int) = rule.onNodeWithTag("digit-$value")

    private fun description(index: Int): String =
        cell(index).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()

    private fun click(text: String) = rule.onNodeWithText(text).performClick()

    @Test fun `I-01 start screen without a save`() {
        launch()
        listOf("やさしい", "ふつう", "むずかしい").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        assertFalse(rule.onAllNodesWithTextExists("続きから"))
    }

    @Test fun `save I-02 a damaged save offers no resume`() {
        store.write("garbage")
        launch()
        assertFalse(rule.onAllNodesWithTextExists("続きから"))
    }

    @Test fun `I-02 entering a digit`() {
        launchPlaying()
        assertTrue(description(EMPTY_CELL), "空き" in description(EMPTY_CELL))
        cell(EMPTY_CELL).performClick()
        digit(4).performClick()
        assertTrue(description(EMPTY_CELL), "記入 4" in description(EMPTY_CELL))
    }

    @Test fun `I-03 entering a memo`() {
        launchPlaying()
        rule.onNodeWithTag("memo").performClick()
        cell(EMPTY_CELL).performClick()
        digit(4).performClick()
        digit(2).performClick()
        assertTrue(description(EMPTY_CELL), "メモ 2 4" in description(EMPTY_CELL))
        assertTrue(description(EMPTY_CELL), "空き" in description(EMPTY_CELL))
    }

    @Test fun `I-04 erase, undo and redo`() {
        launchPlaying()
        cell(EMPTY_CELL).performClick()
        digit(4).performClick()
        click("消す")
        assertTrue("空き" in description(EMPTY_CELL))
        click("戻す")
        assertTrue("記入 4" in description(EMPTY_CELL))
        click("進む")
        assertTrue("空き" in description(EMPTY_CELL))
    }

    @Test fun `I-05 a conflicting digit is marked, a harmless one is not`() {
        launchPlaying()
        cell(EMPTY_CELL).performClick()
        digit(4).performClick()
        assertFalse(description(EMPTY_CELL), "衝突" in description(EMPTY_CELL))
        digit(5).performClick()
        assertTrue(description(EMPTY_CELL), "衝突" in description(EMPTY_CELL))
    }

    @Test fun `I-06 a used-up digit disables its button only`() {
        val blanks = SOLUTION.indices.filter { SOLUTION[it] == 4 }.take(1).toIntArray()
        launchResumed(solvedExcept(*blanks))
        digit(5).assertIsNotEnabled()
        digit(4).assertIsEnabled()
    }

    @Test fun `I-07 the correct last digit clears the game`() {
        launchResumed(solvedExcept(EMPTY_CELL))
        cell(EMPTY_CELL).performClick()
        digit(1).performClick()
        assertFalse(rule.onAllNodesWithTextExists("クリア"))
        digit(1).performClick()
        digit(4).performClick()
        rule.onNodeWithText("クリア").assertIsDisplayed()
        rule.waitUntil { !saveFile.exists() }
    }

    @Test fun `I-08 resume shows the saved board`() {
        launchResumed(GameState(PUZZLE).select(EMPTY_CELL).input(4))
        assertTrue(description(EMPTY_CELL), "記入 4" in description(EMPTY_CELL))
        rule.onNodeWithTag("timer").assertIsDisplayed()
    }

    @Test fun `I-08 every edit reaches the save file`() {
        launchPlaying()
        cell(EMPTY_CELL).performClick()
        digit(4).performClick()
        rule.waitUntil { savedValue(EMPTY_CELL) == 4 }
    }

    @Test fun `I-09 a new game asks before replacing a save`() {
        launch(GameState(PUZZLE).select(EMPTY_CELL).input(4))
        click("やさしい")
        rule.onNodeWithText("新しいゲームを始めますか？").assertIsDisplayed()
        click("キャンセル")
        rule.onNodeWithText("続きから", substring = true).assertIsDisplayed()
        assertTrue(saveFile.exists())
    }

    @Test fun `I-09 no save, no question`() {
        launch()
        click("やさしい")
        waitForBoard()
        assertFalse(rule.onAllNodesWithTextExists("始めますか"))
        rule.waitUntil { saveFile.exists() }
        assertEquals(GIVENS[0], savedValue(0))
    }
}
