package dev.puzzlecollection.nonogram

import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

private fun pattern(text: String) = text.filter { it in "#." }.map { it == '#' }

val HEART = NonogramPuzzle(
    5,
    pattern(
        """
        .#.#.
        #####
        #####
        .###.
        ..#..
        """,
    ),
)

class LineSolverTest {
    @Test fun `U-01 clues`() {
        assertEquals(listOf(2, 1), cluesOf(pattern("##.#")))
        assertEquals(listOf(0), cluesOf(pattern("....")))
        assertEquals(listOf(4), cluesOf(pattern("####")))
    }

    @Test fun `U-02 a line fixes its overlap`() {
        val solved = LineSolver.solveLine(IntArray(5) { LineSolver.UNKNOWN }, listOf(4))!!
        assertArrayEquals(intArrayOf(-1, 1, 1, 1, -1), solved)
    }

    @Test fun `U-02 known cells narrow a line`() {
        val line = intArrayOf(0, -1, -1, -1, -1)
        assertArrayEquals(intArrayOf(0, 1, 1, 1, 1), LineSolver.solveLine(line, listOf(4)))
    }

    @Test fun `U-02 a contradiction is reported`() {
        assertNull(LineSolver.solveLine(intArrayOf(1, 1, 1), listOf(1)))
    }

    @Test fun `U-03 solves a known puzzle`() {
        assertEquals(HEART.solution, LineSolver.solve(5, HEART.rowClues, HEART.columnClues))
    }

    @Test fun `U-04 an ambiguous puzzle does not solve`() {
        val diagonal = NonogramPuzzle(2, pattern("#. .#"))
        assertNull(LineSolver.solve(2, diagonal.rowClues, diagonal.columnClues))
    }
}

class NonogramGeneratorTest {
    @Test fun `U-05 puzzles solve by deduction to their solution`() {
        generated.forEach { (label, puzzle) ->
            assertEquals(label, puzzle.solution, LineSolver.solve(puzzle.size, puzzle.rowClues, puzzle.columnClues))
            assertTrue(label, puzzle.solution.any { it } && !puzzle.solution.all { it })
        }
    }

    @Test fun `U-06 seeds reproduce`() {
        assertEquals(NonogramGenerator.generate(10, Random(2)), NonogramGenerator.generate(10, Random(2)))
        assertNotEquals(NonogramGenerator.generate(10, Random(2)), NonogramGenerator.generate(10, Random(3)))
    }

    @Test fun `U-07 generation stays under the limit`() {
        durations.forEach { (size, millis) ->
            println("U-07 size=$size max=${millis.max()}ms avg=${millis.average().toInt()}ms")
            assertTrue("$size took ${millis.max()} ms", millis.max() < 5_000)
        }
    }

    companion object {
        private val generated = mutableMapOf<String, NonogramPuzzle>()
        private val durations = mutableMapOf<Int, MutableList<Long>>()

        @BeforeClass @JvmStatic fun generateAll() {
            for (size in NONOGRAM_SIZES) for (seed in 1L..30) {
                val start = System.nanoTime()
                generated["size=$size seed=$seed"] = NonogramGenerator.generate(size, Random(seed))
                durations.getOrPut(size) { mutableListOf() } += (System.nanoTime() - start) / 1_000_000
            }
        }
    }
}
