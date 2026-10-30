package dev.puzzlecollection.sudoku

import kotlin.random.Random

/** [attempts] counts the full grids tried, to tell slow digging from many retries. */
data class GeneratedPuzzle(val puzzle: Puzzle, val attempts: Int)

object Generator {
    const val EASY_MIN_GIVENS = 36

    /** Same [random] seed, same puzzle. [onAttempt] runs per attempt; throw from it to cancel. */
    fun generate(
        difficulty: Difficulty,
        random: Random,
        onAttempt: () -> Unit = {},
    ): GeneratedPuzzle {
        var attempts = 0
        while (true) {
            onAttempt()
            attempts++
            val solution = Solver.randomSolution(random)
            val givens = dig(solution, difficulty, random)
            if (Grader.grade(givens) == difficulty) {
                return GeneratedPuzzle(Puzzle(givens, solution, difficulty), attempts)
            }
        }
    }

    private fun dig(solution: List<Int>, difficulty: Difficulty, random: Random): List<Int> {
        val givens = solution.toMutableList()
        var remaining = CELL_COUNT
        // Point-symmetric pairs: `cell` and its mirror are removed together.
        val order = (0..CELL_COUNT / 2).shuffled(random)
        for (cell in order) {
            val mirror = CELL_COUNT - 1 - cell
            val removed = if (cell == mirror) 1 else 2
            if (difficulty == Difficulty.EASY && remaining - removed < EASY_MIN_GIVENS) continue
            givens[cell] = BLANK
            givens[mirror] = BLANK
            if (isAcceptable(givens, difficulty)) {
                remaining -= removed
            } else {
                givens[cell] = solution[cell]
                givens[mirror] = solution[mirror]
            }
        }
        return givens
    }

    private fun isAcceptable(givens: List<Int>, difficulty: Difficulty): Boolean =
        Solver.countSolutions(givens) == 1 &&
            (difficulty == Difficulty.HARD || Grader.grade(givens) <= difficulty)
}
