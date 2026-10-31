package dev.puzzlecollection.game2048

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

fun board(vararg values: Int, score: Int = 0, best: Int = 0, reached: Boolean = false): Game2048State {
    val tiles = values.toList().mapIndexedNotNull { cell, value ->
        if (value == 0) null else Tile(cell + 1, value, cell / 4, cell % 4)
    }
    return Game2048State(tiles, score, best, reached)
}

class Game2048StateTest {
    private val random = Random(1)

    /** The first row after moving left, ignoring the spawned tile elsewhere. */
    private fun leftRow(vararg row: Int): Game2048State {
        val cells = row.toList() + List(12) { 0 }
        return board(*cells.toIntArray()).let { state ->
            // Fill the rest with a pattern-free line so spawns land away from row 0.
            state.move(Direction.LEFT, random)
        }
    }

    private fun Game2048State.row(index: Int) = values.subList(index * 4, index * 4 + 4)

    @Test fun `U-01 tiles slide to the edge`() {
        assertEquals(listOf(2, 4), leftRow(0, 2, 0, 4).row(0).take(2))
    }

    @Test fun `U-02 pairs merge once`() {
        assertEquals(listOf(4, 4), leftRow(2, 2, 2, 2).row(0).take(2))
        assertEquals(listOf(4, 4), leftRow(2, 2, 4, 0).row(0).take(2))
    }

    @Test fun `U-02 the pair nearest the far edge merges first`() {
        assertEquals(listOf(4, 2), leftRow(2, 2, 2, 0).row(0).take(2))
        val right = board(2, 2, 2, 0, *IntArray(12)).move(Direction.RIGHT, random)
        assertEquals(listOf(2, 4), right.row(0).drop(2))
    }

    @Test fun `U-03 all four directions`() {
        val start = board(0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(2, start.move(Direction.UP, random).values[1])
        assertEquals(2, start.move(Direction.DOWN, random).values[13])
        assertEquals(2, start.move(Direction.LEFT, random).values[4])
        assertEquals(2, start.move(Direction.RIGHT, random).values[7])
    }

    @Test fun `U-04 a move that changes nothing returns the same state`() {
        val settled = board(2, 4, 0, 0, *IntArray(12))
        assertSame(settled, settled.move(Direction.LEFT, random))
    }

    @Test fun `U-05 score adds merged values`() {
        assertEquals(12, leftRow(2, 2, 4, 4).score)
    }

    @Test fun `U-06 one tile spawns in an empty cell with the right odds`() {
        var fours = 0
        val rounds = 2_000
        repeat(rounds) { seed ->
            val state = board(0, 2, 0, 0, *IntArray(12)).move(Direction.LEFT, Random(seed))
            assertEquals(2, state.tiles.size)
            val spawned = state.tiles.last()
            assertTrue(spawned.value == 2 || spawned.value == 4)
            assertEquals(2, state.values[0])
            if (spawned.value == 4) fours++
        }
        assertTrue("fours=$fours", fours in rounds * 7 / 100..rounds * 13 / 100)
    }

    @Test fun `U-07 over only when full with no merges`() {
        val locked = intArrayOf(2, 4, 2, 4, 4, 2, 4, 2, 2, 4, 2, 4, 4, 2, 4, 2)
        assertTrue(board(*locked).isOver)
        assertFalse(board(*locked.clone().also { it[1] = 2 }).isOver)
        assertFalse(board(*locked.clone().also { it[0] = 0 }).isOver)
    }

    @Test fun `U-08 reaching 2048 sets the flag`() {
        val before = board(1024, 1024, 0, 0, *IntArray(12))
        assertFalse(before.reached2048)
        assertTrue(before.move(Direction.LEFT, random).reached2048)
    }

    @Test fun `U-09 best follows score but never falls`() {
        assertEquals(4, board(2, 2, 0, 0, *IntArray(12)).move(Direction.LEFT, random).best)
        assertEquals(100, board(2, 2, 0, 0, *IntArray(12), best = 100).move(Direction.LEFT, random).best)
    }

    @Test fun `U-10 seeds reproduce`() {
        assertEquals(Game2048State.newGame(0, Random(5)), Game2048State.newGame(0, Random(5)))
    }

    @Test fun `a new game has two tiles and keeps the best`() {
        val state = Game2048State.newGame(50, Random(1))
        assertEquals(2, state.tiles.size)
        assertEquals(50, state.best)
        assertEquals(0, state.score)
    }
}
