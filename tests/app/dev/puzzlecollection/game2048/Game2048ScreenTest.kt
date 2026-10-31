package dev.puzzlecollection.game2048

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.onAllNodesWithTextExists
import dev.puzzlecollection.ui.PuzzleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import kotlin.random.Random

class SwipeTest {
    @Test fun `U-01 swipe direction`() {
        assertEquals(Direction.RIGHT, dragDirection(100f, 10f, 24f))
        assertEquals(Direction.LEFT, dragDirection(-100f, 10f, 24f))
        assertEquals(Direction.UP, dragDirection(10f, -100f, 24f))
        assertEquals(Direction.DOWN, dragDirection(10f, 100f, 24f))
        assertNull(dragDirection(10f, 10f, 24f))
    }
}

@RunWith(AndroidJUnit4::class)
class Game2048ScreenTest {
    @get:Rule val folder = TemporaryFolder()
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val store by lazy { SaveFile(folder.root, GAME2048_SAVE_PATH) }

    private fun board(vararg values: Int, score: Int = 0, best: Int = 0): Game2048State {
        val tiles = values.toList().mapIndexedNotNull { cell, value ->
            if (value == 0) null else Tile(cell + 1, value, cell / 4, cell % 4)
        }
        return Game2048State(tiles, score, best)
    }

    private fun launch(saved: Game2048State) {
        store.write(Game2048SaveCodec.encode(saved))
        val viewModel = Game2048ViewModel(store, Random(1))
        rule.setContent { PuzzleTheme { Game2048Route(onExit = {}, viewModel = viewModel) } }
        rule.waitUntil { rule.onAllNodesWithTextExists("スコア") }
    }

    private fun swipeLeftOnBoard() {
        rule.onNodeWithTag("board").performTouchInput { swipeLeft() }
        rule.waitForIdle()
    }

    @Test fun `I-01 a swipe merges and scores`() {
        launch(board(2, 2, 0, 0, *IntArray(12)))
        swipeLeftOnBoard()
        rule.onNodeWithText("スコア 4").assertIsDisplayed()
        rule.onNodeWithText("ベスト 4").assertIsDisplayed()
    }

    @Test fun `I-02 reaching 2048 is announced and can continue`() {
        launch(board(1024, 1024, 0, 0, *IntArray(12)))
        swipeLeftOnBoard()
        rule.onNodeWithText("2048 達成").assertIsDisplayed()
        rule.onNodeWithText("続ける").performClick()
        assertFalse(rule.onAllNodesWithTextExists("2048 達成"))
    }

    @Test fun `I-03 a locked board ends the game`() {
        launch(board(2, 4, 2, 4, 4, 2, 4, 2, 2, 4, 2, 4, 4, 2, 4, 2))
        rule.onNodeWithText("ゲームオーバー").assertIsDisplayed()
    }

    @Test fun `I-04 best survives a finished game`() {
        launch(board(2, 4, 2, 4, 4, 2, 4, 2, 2, 4, 2, 4, 4, 2, 4, 2, score = 10, best = 500))
        rule.onAllNodesWithText("新しいゲーム")[1].performClick()
        rule.onNodeWithText("ベスト 500").assertIsDisplayed()
        rule.onNodeWithText("スコア 0").assertIsDisplayed()
    }

    @Test fun `I-05 a new game asks first`() {
        launch(board(2, 2, 0, 0, *IntArray(12), score = 8, best = 8))
        rule.onNodeWithText("新しいゲーム").performClick()
        rule.onNodeWithText("キャンセル").performClick()
        rule.onNodeWithText("スコア 8").assertIsDisplayed()
    }
}
