package dev.puzzlecollection.numberlink

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Found by BruteForceCounter: exactly one / exactly two solutions on a 4x4 board. */
private val UNIQUE_4 = listOf(3 to 5, 13 to 7, 12 to 9)
private val TWO_4 = listOf(6 to 14, 12 to 8, 0 to 7)

class SolverTest {
    @Test fun `U-01 a unique board counts one`() {
        assertEquals(1, BruteForceCounter.count(4, UNIQUE_4))
        assertEquals(1, NumberlinkSolver.countSolutions(4, UNIQUE_4))
    }

    @Test fun `U-02 a board with two solutions counts two`() {
        assertEquals(2, BruteForceCounter.count(4, TWO_4, limit = 3))
        assertEquals(2, NumberlinkSolver.countSolutions(4, TWO_4, limit = 3))
    }

    @Test fun `U-03 pruning agrees with brute force`() {
        val random = Random(42)
        var checked = 0
        var nonZero = 0
        repeat(600) {
            val size = if (it % 4 == 0) 5 else 4
            val pairs = random.nextInt(2, 6)
            val cells = (0 until size * size).shuffled(random).take(pairs * 2)
            val endpoints = List(pairs) { cells[it * 2] to cells[it * 2 + 1] }
            val brute = BruteForceCounter.count(size, endpoints)
            val pruned = NumberlinkSolver.countSolutions(size, endpoints)
            assertEquals("size=$size $endpoints", brute, pruned)
            checked++
            if (brute > 0) nonZero++
        }
        println("U-03 checked=$checked withSolutions=$nonZero")
    }

    @Test fun `U-04 an exhausted budget is unknown, not unique`() {
        assertNull(NumberlinkSolver.countSolutions(5, listOf(0 to 24), nodeBudget = 3))
    }
}
