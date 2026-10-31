package dev.puzzlecollection.slide

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideStateTest {
    private val solved = SlideState.solved(4)

    @Test fun `U-01 the solved board`() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 0), SlideState.solved(3).tiles)
        assertTrue(solved.isSolved)
    }

    @Test fun `U-02 generated boards are solvable and not solved`() {
        for (size in SLIDE_SIZES) for (seed in 1L..50) {
            val state = SlideGenerator.generate(size, Random(seed))
            assertFalse(state.isSolved)
            assertEquals(0, state.moves)
            assertEquals((0 until size * size).toList(), state.tiles.sorted())
            assertTrue("size=$size seed=$seed", isSolvableByParity(state))
        }
    }

    @Test fun `U-02 the parity check rejects a swapped pair`() {
        val swapped = solved.tiles.toMutableList().also { it[0] = 2; it[1] = 1 }
        assertFalse(isSolvableByParity(SlideState(4, swapped)))
    }

    /** Independent of the generator: the classic inversion-count rule. */
    private fun isSolvableByParity(state: SlideState): Boolean {
        val tiles = state.tiles.filter { it != BLANK_TILE }
        val inversions = tiles.indices.sumOf { i -> (i + 1 until tiles.size).count { tiles[i] > tiles[it] } }
        val rowOffset = if (state.size % 2 == 0) state.size - 1 - state.blank / state.size else 0
        return (inversions + rowOffset) % 2 == 0
    }

    private val midGame = SlideState(4, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 13, 14, 15))

    @Test fun `U-03 an adjacent tile swaps with the gap`() {
        val next = midGame.tap(13)
        assertEquals(13, next.tiles[12])
        assertEquals(BLANK_TILE, next.tiles[13])
        assertEquals(1, next.moves)
    }

    @Test fun `U-04 a distant tile pushes the line as one move`() {
        val next = midGame.tap(15)
        assertEquals(listOf(13, 14, 15, 0), next.tiles.subList(12, 16))
        assertEquals(1, next.moves)
    }

    @Test fun `U-04 columns slide too`() {
        val next = midGame.tap(4)
        assertEquals(5, next.tiles[8])
        assertEquals(9, next.tiles[12])
        assertEquals(BLANK_TILE, next.tiles[4])
    }

    @Test fun `U-05 tiles off the line and the gap itself are ignored`() {
        assertSame(midGame, midGame.tap(9))
        assertSame(midGame, midGame.tap(12))
    }

    @Test fun `U-06 a board one move away is not solved`() {
        assertFalse(solved.copy(tiles = midGame.tiles).isSolved)
        assertTrue(SlideState(4, solved.tiles.toMutableList().also { it[15] = 15; it[14] = 0 }).tap(15).isSolved)
    }

    @Test fun `U-07 a solved board ignores taps`() {
        assertSame(solved, solved.tap(14))
    }

    @Test fun `U-08 seeds reproduce`() {
        assertEquals(SlideGenerator.generate(4, Random(3)), SlideGenerator.generate(4, Random(3)))
        assertNotEquals(SlideGenerator.generate(4, Random(3)), SlideGenerator.generate(4, Random(4)))
    }
}
