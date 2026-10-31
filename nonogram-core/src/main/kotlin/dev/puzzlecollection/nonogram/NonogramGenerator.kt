package dev.puzzlecollection.nonogram

import kotlin.random.Random

object NonogramGenerator {
    private const val FILL_CHANCE = 0.6
    private const val MIN_FILLED = 0.35
    private const val MAX_FILLED = 0.8

    /** Same [random] seed, same puzzle. [onAttempt] runs per attempt; throw from it to cancel. */
    fun generate(size: Int, random: Random, onAttempt: () -> Unit = {}): NonogramPuzzle {
        while (true) {
            onAttempt()
            val puzzle = NonogramPuzzle(size, smoothed(size, random))
            val filled = puzzle.solution.count { it }.toDouble() / puzzle.solution.size
            if (filled !in MIN_FILLED..MAX_FILLED) continue
            if (LineSolver.solve(size, puzzle.rowClues, puzzle.columnClues) != null) return puzzle
        }
    }

    /** Pure noise is almost never line-solvable; one majority pass forms solid shapes. */
    private fun smoothed(size: Int, random: Random): List<Boolean> {
        val noise = List(size * size) { random.nextDouble() < FILL_CHANCE }
        return List(size * size) { cell ->
            val row = cell / size
            val column = cell % size
            var filled = 0
            var total = 0
            for (r in row - 1..row + 1) for (c in column - 1..column + 1) {
                if (r !in 0 until size || c !in 0 until size) continue
                total++
                if (noise[r * size + c]) filled++
            }
            filled * 2 > total
        }
    }
}
