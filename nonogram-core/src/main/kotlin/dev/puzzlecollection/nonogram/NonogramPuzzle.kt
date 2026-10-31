package dev.puzzlecollection.nonogram

val NONOGRAM_SIZES = listOf(5, 10, 15)

/** [solution] is row-major with [size] squared elements. */
data class NonogramPuzzle(val size: Int, val solution: List<Boolean>) {
    val rowClues: List<List<Int>> by lazy { List(size) { cluesOf(row(solution, size, it)) } }
    val columnClues: List<List<Int>> by lazy { List(size) { cluesOf(column(solution, size, it)) } }
}

internal fun <T> row(cells: List<T>, size: Int, index: Int): List<T> =
    cells.subList(index * size, (index + 1) * size)

internal fun <T> column(cells: List<T>, size: Int, index: Int): List<T> =
    List(size) { cells[it * size + index] }

/** Run lengths in order. An empty line has the single clue `0`, as printed puzzles show it. */
fun cluesOf(line: List<Boolean>): List<Int> {
    val runs = mutableListOf<Int>()
    var run = 0
    for (filled in line) {
        if (filled) {
            run++
        } else if (run > 0) {
            runs += run
            run = 0
        }
    }
    if (run > 0) runs += run
    return runs.ifEmpty { listOf(0) }
}
