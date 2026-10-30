package dev.puzzlecollection.sudoku

const val CELL_COUNT = 81
const val BLANK = 0
val DIGITS = 1..9

/** Cell index layout: `row * 9 + column`. Save data depends on this order. */
object Geometry {
    /** 9 rows, then 9 columns, then 9 boxes. */
    val units: List<IntArray> = buildList {
        for (r in 0 until 9) add(IntArray(9) { c -> r * 9 + c })
        for (c in 0 until 9) add(IntArray(9) { r -> r * 9 + c })
        for (b in 0 until 9) add(IntArray(9) { i -> boxOf(b, i) })
    }

    val unitsOfCell: List<List<IntArray>> =
        List(CELL_COUNT) { cell -> units.filter { cell in it } }

    val peers: List<IntArray> = List(CELL_COUNT) { cell ->
        unitsOfCell[cell].flatMap { it.asIterable() }.distinct().filter { it != cell }.toIntArray()
    }

    internal fun boxIndex(cell: Int): Int = (cell / 27) * 3 + (cell % 9) / 3

    private fun boxOf(box: Int, i: Int): Int =
        (box / 3 * 3 + i / 3) * 9 + box % 3 * 3 + i % 3
}

fun digitBit(digit: Int): Int = 1 shl digit
