package dev.puzzlecollection

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.game2048.GAME2048_SAVE_PATH
import dev.puzzlecollection.game2048.Game2048Route
import dev.puzzlecollection.game2048.Game2048SaveCodec
import dev.puzzlecollection.game2048.Game2048State
import dev.puzzlecollection.game2048.Game2048ViewModel
import dev.puzzlecollection.game2048.Tile
import dev.puzzlecollection.nonogram.Mark
import dev.puzzlecollection.nonogram.NONOGRAM_SAVE_PATH
import dev.puzzlecollection.nonogram.NonogramGenerator
import dev.puzzlecollection.nonogram.NonogramRoute
import dev.puzzlecollection.nonogram.NonogramSaveCodec
import dev.puzzlecollection.nonogram.NonogramState
import dev.puzzlecollection.nonogram.NonogramViewModel
import dev.puzzlecollection.slide.SLIDE_SAVE_PATH
import dev.puzzlecollection.slide.SlideGenerator
import dev.puzzlecollection.slide.SlideRoute
import dev.puzzlecollection.slide.SlideSaveCodec
import dev.puzzlecollection.slide.SlideViewModel
import dev.puzzlecollection.ui.PuzzleTheme
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders each game for a person to look at. Asserts nothing. See ScreenshotCapture. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class GameScreenshots {
    @get:Rule val folder = TemporaryFolder()
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun resume() {
        rule.waitUntil { rule.onAllNodesWithTextExists("続きから") }
        rule.onNodeWithText("続きから", substring = true).performClick()
        rule.waitForIdle()
    }

    @Test fun slide() {
        val file = SaveFile(folder.root, SLIDE_SAVE_PATH)
        file.write(SlideSaveCodec.encode(SlideGenerator.generate(4, Random(1)), 42_000))
        val viewModel = SlideViewModel(file, clock = { 0L })
        rule.setContent { PuzzleTheme { SlideRoute({}, viewModel) } }
        resume()
        capture("slide")
    }

    @Test fun game2048() {
        val file = SaveFile(folder.root, GAME2048_SAVE_PATH)
        val values = listOf(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 0, 0, 2, 0, 4)
        val tiles = values.mapIndexedNotNull { cell, value ->
            if (value == 0) null else Tile(cell + 1, value, cell / 4, cell % 4)
        }
        file.write(Game2048SaveCodec.encode(Game2048State(tiles, 1204, 5320, reached2048 = true)))
        val viewModel = Game2048ViewModel(file)
        rule.setContent { PuzzleTheme { Game2048Route({}, viewModel) } }
        rule.waitUntil { rule.onAllNodesWithTextExists("スコア") }
        capture("game2048")
    }

    private fun nonogram(size: Int, name: String) {
        val file = SaveFile(folder.root, NONOGRAM_SAVE_PATH)
        val puzzle = NonogramGenerator.generate(size, Random(1))
        val cells = puzzle.solution.mapIndexed { i, filled ->
            when {
                i >= size * size * 2 / 3 -> Mark.EMPTY
                filled -> Mark.FILLED
                else -> Mark.CROSS
            }
        }
        file.write(NonogramSaveCodec.encode(NonogramState(puzzle, cells), 42_000))
        val viewModel = NonogramViewModel(file, clock = { 0L })
        rule.setContent { PuzzleTheme { NonogramRoute({}, viewModel) } }
        resume()
        capture(name)
        rule.onNodeWithTag("nonogram-board").performTouchInput {
            pinch(center - Offset(10f, 0f), center - Offset(180f, 0f), center + Offset(10f, 0f), center + Offset(180f, 0f))
        }
        capture("${name}_zoomed")
    }

    @Test fun nonogram20() = nonogram(20, "nonogram20")
}
