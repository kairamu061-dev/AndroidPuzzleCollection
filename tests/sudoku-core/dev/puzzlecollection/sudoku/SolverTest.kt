package dev.puzzlecollection.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SolverTest {
    @Test fun `U-01 solves a known puzzle`() {
        assertEquals(SAMPLE_SOLUTION, Solver.solve(SAMPLE_GIVENS))
    }

    @Test fun `U-02 a unique puzzle counts one`() {
        assertEquals(1, Solver.countSolutions(SAMPLE_GIVENS))
    }

    @Test fun `U-02 a deadly rectangle counts two`() {
        assertEquals(2, Solver.countSolutions(withDeadlyRectangleBlanked(SAMPLE_SOLUTION)))
    }

    @Test fun `U-02 the count stops at the limit`() {
        assertEquals(5, Solver.countSolutions(List(CELL_COUNT) { BLANK }, limit = 5))
    }

    @Test fun `U-03 a board that breaks the rules has no solutions`() {
        val broken = SAMPLE_GIVENS.toMutableList().also { it[R0C2] = 5 }
        assertEquals(0, Solver.countSolutions(broken))
        assertNull(Solver.solve(broken))
    }

    /** Blanks four cells that can swap their two digits, leaving the board with two solutions. */
    private fun withDeadlyRectangleBlanked(solution: List<Int>): List<Int> {
        for (r1 in 0 until 9) for (r2 in r1 + 1 until 9) {
            if (r1 / 3 != r2 / 3) continue
            for (c1 in 0 until 9) for (c2 in c1 + 1 until 9) {
                val a = r1 * 9 + c1
                val b = r1 * 9 + c2
                val c = r2 * 9 + c1
                val d = r2 * 9 + c2
                if (solution[a] == solution[d] && solution[b] == solution[c]) {
                    return solution.toMutableList().also { for (i in listOf(a, b, c, d)) it[i] = BLANK }
                }
            }
        }
        error("the fixture has no deadly rectangle")
    }
}
