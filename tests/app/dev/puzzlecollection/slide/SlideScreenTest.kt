package dev.puzzlecollection.slide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.onAllNodesWithTextExists
import dev.puzzlecollection.ui.PuzzleTheme
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SlideScreenTest {
    @get:Rule val folder = TemporaryFolder()
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val store by lazy { SaveFile(folder.root, SLIDE_SAVE_PATH) }
    private val file get() = File(folder.root, SLIDE_SAVE_PATH)
    private val oneMoveLeft = SlideState(3, listOf(1, 2, 3, 4, 5, 6, 7, 0, 8))

    private fun launch(saved: SlideState? = null) {
        saved?.let { store.write(SlideSaveCodec.encode(it, 1_000)) }
        val viewModel = SlideViewModel(store, clock = { 0L }, generate = { oneMoveLeft })
        rule.setContent { PuzzleTheme { SlideRoute(onExit = {}, viewModel = viewModel) } }
        rule.waitUntil { rule.onAllNodesWithTextExists("新しいゲーム") }
    }

    private fun resume() {
        rule.onNodeWithText("続きから", substring = true).performClick()
        rule.waitUntil { rule.onAllNodesWithTextExists("手数") }
    }

    @Test fun `I-01 start screen`() {
        launch()
        listOf("3×3", "4×4", "5×5").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        assertFalse(rule.onAllNodesWithTextExists("続きから"))
    }

    @Test fun `I-02 tapping beside the gap moves and counts`() {
        launch(SlideState(3, listOf(1, 2, 3, 4, 0, 5, 7, 8, 6)))
        resume()
        rule.onNodeWithTag("tile-5").performClick()
        rule.onNodeWithTag("status").assertTextContains("手数 1", substring = true)
    }

    @Test fun `I-03 the last move clears, another tile does not`() {
        launch(oneMoveLeft)
        resume()
        rule.onNodeWithTag("tile-4").performClick()
        assertFalse(rule.onAllNodesWithTextExists("クリア"))
        rule.onNodeWithTag("tile-8").performClick()
        rule.onNodeWithText("クリア").assertIsDisplayed()
        rule.waitUntil { !file.exists() }
    }

    @Test fun `I-04 resume restores the move count`() {
        launch(oneMoveLeft.copy(moves = 7))
        resume()
        rule.onNodeWithTag("status").assertTextContains("手数 7", substring = true)
    }

    @Test fun `I-05 a new game asks first`() {
        launch(oneMoveLeft)
        rule.onNodeWithText("4×4").performClick()
        rule.onNodeWithText("新しいゲームを始めますか？").assertIsDisplayed()
        rule.onNodeWithText("キャンセル").performClick()
        assertTrue(file.exists())
    }
}
