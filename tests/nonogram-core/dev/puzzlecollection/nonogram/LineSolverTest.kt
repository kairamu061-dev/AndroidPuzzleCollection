package dev.puzzlecollection.nonogram

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
