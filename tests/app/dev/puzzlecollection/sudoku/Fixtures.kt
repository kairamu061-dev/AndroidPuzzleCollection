package dev.puzzlecollection.sudoku

private fun digits(text: String) = text.filter { it.isDigit() }.map { it - '0' }

val GIVENS = digits(
    "530070000 600195000 098000060 800060003 400803001 700020006 060000280 000419005 000080079",
)
val SOLUTION = digits(
    "534678912 672195348 198342567 859761423 426853791 713924856 961537284 287419635 345286179",
)
val PUZZLE = Puzzle(GIVENS, SOLUTION, Difficulty.MEDIUM)
const val EMPTY_CELL = 2 // solution 4

fun solvedExcept(vararg blank: Int): GameState {
    val values = SOLUTION.toMutableList()
    blank.forEach { values[it] = BLANK }
    return GameState(PUZZLE, BoardSnapshot(values, List(CELL_COUNT) { 0 }))
}
