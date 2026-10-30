package dev.puzzlecollection.sudoku

/** Declaration order is the order of strictness: [Generator] compares with `<=`. */
enum class Difficulty { EASY, MEDIUM, HARD }

/** [givens] and [solution] have [CELL_COUNT] elements; [BLANK] marks an empty given. */
data class Puzzle(
    val givens: List<Int>,
    val solution: List<Int>,
    val difficulty: Difficulty,
)
