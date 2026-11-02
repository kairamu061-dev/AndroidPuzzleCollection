package dev.puzzlecollection.numberlink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 4x4, three lines:      0  1  2  3
 *   line 0: 0-1-2-3-7    4  5  6  7
 *   line 1: 4-5-6-10-11-15-14   8 9 10 11
 *   line 2: 8-12-13-9    12 13 14 15
 */
val SAMPLE = NumberlinkPuzzle(4, listOf(listOf(0, 1, 2, 3, 7), listOf(4, 5, 6, 10, 11, 15, 14), listOf(8, 12, 13, 9)))

fun NumberlinkState.draw(vararg cells: Int): NumberlinkState {
    var state = strokeStart(cells.first())
    cells.drop(1).forEach { state = state.strokeTo(it) }
    return state.strokeEnd()
}

class StateTest {
    private val fresh = NumberlinkState(SAMPLE)

    @Test fun `U-08 starting at an endpoint, inside a path, and on an empty cell`() {
        val started = fresh.strokeStart(0)
        assertEquals(listOf(0), started.paths[0])
        assertEquals(0, started.active)
        val drawn = fresh.draw(0, 1, 2, 3)
        assertEquals(listOf(0, 1), drawn.strokeStart(1).paths[0])
        assertSame(fresh, fresh.strokeStart(5))
    }

    @Test fun `U-09 extending and stopping at the partner`() {
        val joined = fresh.draw(0, 1, 2, 3, 7)
        assertTrue(joined.isConnected(0))
        val beyond = fresh.strokeStart(0).strokeTo(1).strokeTo(2).strokeTo(3).strokeTo(7).strokeTo(6)
        assertEquals(listOf(0, 1, 2, 3, 7), beyond.paths[0])
    }

    @Test fun `U-09 starting from the far endpoint works too`() {
        assertTrue(fresh.draw(7, 3, 2, 1, 0).isConnected(0))
    }

    @Test fun `U-10 going back shortens the path`() {
        val state = fresh.strokeStart(0).strokeTo(1).strokeTo(2).strokeTo(3).strokeTo(1)
        assertEquals(listOf(0, 1), state.paths[0])
    }

    @Test fun `U-11 crossing another line cuts it there`() {
        val other = fresh.draw(4, 5, 6)
        val state = other.draw(0, 1, 5)
        assertEquals(listOf(4), state.paths[1])
        assertEquals(listOf(0, 1, 5), state.paths[0])
    }

    @Test fun `U-11 another line's endpoint blocks`() {
        val state = fresh.strokeStart(0).strokeTo(4)
        assertEquals(listOf(0), state.paths[0])
    }

    @Test fun `U-12 a jump to a distant cell is ignored`() {
        val state = fresh.strokeStart(0).strokeTo(2)
        assertEquals(listOf(0), state.paths[0])
        assertEquals(listOf(0, 1), state.strokeTo(1).paths[0])
    }

    @Test fun `U-13 one stroke is one undo, and no change adds nothing`() {
        val drawn = fresh.draw(0, 1, 2)
        assertEquals(1, drawn.undoStack.size)
        assertEquals(fresh.paths, drawn.undo().paths)
        assertEquals(drawn.paths, drawn.undo().redo().paths)
        assertEquals(drawn.undoStack.size, drawn.draw(5).undoStack.size)
    }

    @Test fun `U-14 the rules decide completion, not the stored solution`() {
        assertTrue(NumberlinkState(SAMPLE, SAMPLE.lines).isSolved)
        // TWO_4 from SolverTest: whichever of its two solutions the player draws, it is a clear.
        val endpoints = listOf(6 to 14, 12 to 8, 0 to 7)
        val solutions = NumberlinkSolver.findSolutions(4, endpoints)!!
        assertEquals(2, solutions.size)
        val stored = NumberlinkPuzzle(4, pathsOf(endpoints, solutions[0]))
        assertTrue(NumberlinkState(stored, pathsOf(endpoints, solutions[1])).isSolved)
    }

    @Test fun `U-15 joined lines with an empty cell are not a clear`() {
        val short = NumberlinkState(SAMPLE, listOf(listOf(0, 1, 2, 3, 7), listOf(4, 5, 6, 10, 14), listOf(8, 12, 13, 9)))
        assertTrue(short.allConnected)
        assertEquals(2, short.emptyCells)
        assertFalse(short.isSolved)
    }

    @Test fun `a solved board ignores edits`() {
        val solved = NumberlinkState(SAMPLE, SAMPLE.lines)
        assertSame(solved, solved.strokeStart(0))
        assertSame(solved, solved.undo())
    }

    private fun pathsOf(endpoints: List<Pair<Int, Int>>, edges: Set<Long>): List<List<Int>> =
        endpoints.map { (start, end) ->
            val path = mutableListOf(start)
            while (path.last() != end) {
                val head = path.last()
                path += neighborsOf(head, 4).first {
                    it !in path && NumberlinkSolver.edgeKey(head, it) in edges
                }
            }
            path
        }
}
