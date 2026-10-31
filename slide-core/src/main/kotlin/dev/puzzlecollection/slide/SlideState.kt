package dev.puzzlecollection.slide

import kotlin.math.abs
import kotlin.random.Random

const val BLANK_TILE = 0
val SLIDE_SIZES = 3..5

/** [tiles] is row-major with [size] squared elements; [BLANK_TILE] marks the gap. */
data class SlideState(val size: Int, val tiles: List<Int>, val moves: Int = 0) {
    val blank: Int get() = tiles.indexOf(BLANK_TILE)

    val isSolved: Boolean get() = tiles == solvedTiles(size)

    /** Slides every tile between [cell] and the gap. Ignored when not in the gap's line. */
    fun tap(cell: Int): SlideState {
        if (isSolved || cell == blank) return this
        val gap = blank
        val sameRow = cell / size == gap / size
        val sameColumn = cell % size == gap % size
        if (!sameRow && !sameColumn) return this
        val step = (if (sameRow) 1 else size) * if (cell > gap) 1 else -1
        val next = tiles.toMutableList()
        var at = gap
        while (at != cell) {
            next[at] = next[at + step]
            at += step
        }
        next[cell] = BLANK_TILE
        return copy(tiles = next, moves = moves + 1)
    }

    companion object {
        fun solved(size: Int) = SlideState(size, solvedTiles(size))

        private fun solvedTiles(size: Int): List<Int> =
            List(size * size) { (it + 1) % (size * size) }
    }
}

object SlideGenerator {
    /** Walks the gap through legal moves, so the result can always be solved. */
    fun generate(size: Int, random: Random): SlideState {
        var tiles = SlideState.solved(size)
        val steps = size * size * size * 20
        var previous = -1
        repeat(steps) {
            val gap = tiles.blank
            val options = neighbors(gap, size).filter { it != previous }
            previous = gap
            tiles = tiles.copy(tiles = swapped(tiles.tiles, gap, options.random(random)))
        }
        return if (tiles.isSolved) generate(size, random) else tiles.copy(moves = 0)
    }

    // Not `tap`: it refuses to move a board that happens to pass through the solved state.
    private fun swapped(tiles: List<Int>, a: Int, b: Int): List<Int> =
        tiles.toMutableList().also {
            it[a] = tiles[b]
            it[b] = tiles[a]
        }

    private fun neighbors(cell: Int, size: Int): List<Int> =
        (0 until size * size).filter {
            abs(it / size - cell / size) + abs(it % size - cell % size) == 1
        }
}
