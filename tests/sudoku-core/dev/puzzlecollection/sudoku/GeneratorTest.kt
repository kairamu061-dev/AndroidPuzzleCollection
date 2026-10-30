package dev.puzzlecollection.sudoku

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class GeneratorTest {
    @Test fun `U-04 solutions obey the rules`() = forEachGenerated { generated, label ->
        val solution = generated.puzzle.solution
        Geometry.units.forEach { unit ->
            assertEquals(label, DIGITS.toSet(), unit.map { solution[it] }.toSet())
        }
    }

    @Test fun `U-05 puzzles have one solution matching their givens`() = forEachGenerated { generated, label ->
        val puzzle = generated.puzzle
        assertEquals(label, 1, Solver.countSolutions(puzzle.givens))
        assertEquals(label, puzzle.solution, Solver.solve(puzzle.givens))
    }

    @Test fun `U-06 the same seed reproduces the puzzle`() {
        val first = Generator.generate(Difficulty.MEDIUM, Random(7))
        assertEquals(first, Generator.generate(Difficulty.MEDIUM, Random(7)))
        assertNotEquals(first.puzzle, Generator.generate(Difficulty.MEDIUM, Random(8)).puzzle)
    }

    @Test fun `U-08 puzzles grade as requested`() = forEachGenerated { generated, label ->
        val puzzle = generated.puzzle
        assertEquals(label, puzzle.difficulty, Grader.grade(puzzle.givens))
        if (puzzle.difficulty == Difficulty.EASY) {
            assertTrue(label, puzzle.givens.count { it != BLANK } >= Generator.EASY_MIN_GIVENS)
        }
    }

    @Test fun `U-09 generation stays under the limit`() {
        durations.forEach { (difficulty, millis) ->
            println(
                "U-09 $difficulty max=${millis.max()}ms avg=${millis.average().toInt()}ms " +
                    "maxAttempts=${attempts.getValue(difficulty).max()}",
            )
            assertTrue("$difficulty took ${millis.max()} ms", millis.max() < LIMIT_MILLIS)
        }
    }

    private fun forEachGenerated(check: (GeneratedPuzzle, String) -> Unit) {
        generated.forEach { (label, puzzle) -> check(puzzle, label) }
    }

    companion object {
        private const val SEEDS = 50
        private const val LIMIT_MILLIS = 5_000L
        private val generated = mutableMapOf<String, GeneratedPuzzle>()
        private val durations = mutableMapOf<Difficulty, MutableList<Long>>()
        private val attempts = mutableMapOf<Difficulty, MutableList<Int>>()

        @BeforeClass @JvmStatic fun generateAll() {
            for (difficulty in Difficulty.entries) {
                for (seed in 1L..SEEDS) {
                    val start = System.nanoTime()
                    val result = Generator.generate(difficulty, Random(seed))
                    durations.getOrPut(difficulty) { mutableListOf() } +=
                        (System.nanoTime() - start) / 1_000_000
                    attempts.getOrPut(difficulty) { mutableListOf() } += result.attempts
                    generated["$difficulty seed=$seed"] = result
                }
            }
        }
    }
}
