package dev.puzzlecollection.sudoku

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.puzzlecollection.onAllNodesWithTextExists
import dev.puzzlecollection.ui.PuzzleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the play screen for a human or agent to look at. Asserts nothing.
 * Run with: ./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true --tests '*ScreenshotCapture*'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotCapture {
    @get:Rule val folder = TemporaryFolder()
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val playedGame = GameState(PUZZLE)
        .select(EMPTY_CELL).input(4)
        .select(3).toggleMemoMode().input(1).input(2).input(6).input(9).toggleMemoMode()
        .select(10).input(8)
        .select(40).input(5)

    private fun capture(name: String) {
        val repository = SaveRepository(folder.root).apply { write(SavedGame(playedGame, 83_000)) }
        val viewModel = SudokuViewModel(repository, clock = { 0L })
        rule.setContent { PuzzleTheme { SudokuRoute(onExit = {}, viewModel = viewModel) } }
        rule.waitUntil { rule.onAllNodesWithTextExists("続きから") }
        rule.runOnUiThread {
            viewModel.resumeGame()
            viewModel.selectCell(40)
        }
        rule.waitUntil { rule.onAllNodesWithTextExists("メモ") }
        rule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    @Test fun portrait() = capture("portrait")

    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun smallPhone() = capture("small")

    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    @Test fun dark() = capture("dark")

    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    @Test fun landscape() = capture("landscape")
}
