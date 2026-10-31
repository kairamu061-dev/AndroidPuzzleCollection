package dev.puzzlecollection.game2048

import kotlin.random.Random

const val BOARD_SIZE = 4
const val WIN_VALUE = 2048
private const val FOUR_CHANCE = 0.1

enum class Direction { UP, DOWN, LEFT, RIGHT }

/** [id] keys the on-screen animation; a merge keeps the id of the tile nearer the far edge. */
data class Tile(val id: Int, val value: Int, val row: Int, val column: Int)

data class Game2048State(
    val tiles: List<Tile>,
    val score: Int = 0,
    val best: Int = 0,
    val reached2048: Boolean = false,
    val nextId: Int = tiles.maxOfOrNull { it.id + 1 } ?: 1,
) {
    /** Row-major values, 0 for empty. Ignores ids, so it compares boards. */
    val values: List<Int>
        get() = MutableList(BOARD_SIZE * BOARD_SIZE) { 0 }.also { cells ->
            tiles.forEach { cells[it.row * BOARD_SIZE + it.column] = it.value }
        }

    val isOver: Boolean
        get() = tiles.size == BOARD_SIZE * BOARD_SIZE &&
            Direction.entries.none { slide(it).values != values }

    /** Returns `this` for a move that changes nothing; such a move must not spawn a tile. */
    fun move(direction: Direction, random: Random): Game2048State {
        val slid = slide(direction)
        return if (slid.values == values) this else slid.spawn(random)
    }

    private fun slide(direction: Direction): Game2048State {
        val moved = mutableListOf<Tile>()
        var gained = 0
        for (line in 0 until BOARD_SIZE) {
            val inLine = lineCells(direction, line).mapNotNull { (row, column) ->
                tiles.firstOrNull { it.row == row && it.column == column }
            }
            val targets = lineCells(direction, line)
            var next = 0
            var i = 0
            while (i < inLine.size) {
                val tile = inLine[i]
                val (row, column) = targets[next++]
                val merges = inLine.getOrNull(i + 1)?.value == tile.value
                val value = if (merges) tile.value * 2 else tile.value
                if (merges) gained += value
                moved += tile.copy(value = value, row = row, column = column)
                i += if (merges) 2 else 1
            }
        }
        val newScore = score + gained
        return copy(
            tiles = moved,
            score = newScore,
            best = maxOf(best, newScore),
            reached2048 = reached2048 || moved.any { it.value >= WIN_VALUE },
        )
    }

    fun spawn(random: Random): Game2048State {
        val cells = values
        val empty = cells.indices.filter { cells[it] == 0 }
        val cell = empty.random(random)
        val value = if (random.nextDouble() < FOUR_CHANCE) 4 else 2
        val tile = Tile(nextId, value, cell / BOARD_SIZE, cell % BOARD_SIZE)
        return copy(tiles = tiles + tile, nextId = nextId + 1)
    }

    companion object {
        fun newGame(best: Int, random: Random): Game2048State =
            Game2048State(emptyList(), best = best).spawn(random).spawn(random)

        /** Cells of one line, ordered from the edge the tiles move toward. */
        private fun lineCells(direction: Direction, line: Int): List<Pair<Int, Int>> {
            val far = BOARD_SIZE - 1
            return (0 until BOARD_SIZE).map { i ->
                when (direction) {
                    Direction.LEFT -> line to i
                    Direction.RIGHT -> line to far - i
                    Direction.UP -> i to line
                    Direction.DOWN -> far - i to line
                }
            }
        }
    }
}
