package dev.puzzlecollection.sudoku

import kotlin.random.Random

object Solver {
    fun solve(cells: List<Int>): List<Int>? {
        val search = Search.from(cells) ?: return null
        return if (search.run(limit = 1) == 1) search.firstSolution?.toList() else null
    }

    /** Counts solutions up to [limit]; a board that already breaks the rules has 0. */
    fun countSolutions(cells: List<Int>, limit: Int = 2): Int =
        Search.from(cells)?.run(limit) ?: 0

    /** A random complete grid. */
    fun randomSolution(random: Random): List<Int> {
        val search = Search.from(List(CELL_COUNT) { BLANK })!!
        search.digitOrder = DIGITS.shuffled(random).toIntArray()
        search.run(limit = 1)
        return search.firstSolution!!.toList()
    }

    private class Search(
        private val cells: IntArray,
        private val rows: IntArray,
        private val columns: IntArray,
        private val boxes: IntArray,
    ) {
        var digitOrder: IntArray = DIGITS.toList().toIntArray()
        var firstSolution: IntArray? = null
        private var found = 0
        private var limit = 0

        fun run(limit: Int): Int {
            this.limit = limit
            found = 0
            descend()
            return found
        }

        private fun descend() {
            val cell = mostConstrainedCell()
            if (cell < 0) {
                if (found++ == 0) firstSolution = cells.copyOf()
                return
            }
            val row = cell / 9
            val column = cell % 9
            val box = Geometry.boxIndex(cell)
            val used = rows[row] or columns[column] or boxes[box]
            for (digit in digitOrder) {
                val bit = digitBit(digit)
                if (used and bit != 0) continue
                cells[cell] = digit
                rows[row] = rows[row] or bit
                columns[column] = columns[column] or bit
                boxes[box] = boxes[box] or bit
                descend()
                cells[cell] = BLANK
                rows[row] = rows[row] and bit.inv()
                columns[column] = columns[column] and bit.inv()
                boxes[box] = boxes[box] and bit.inv()
                if (found >= limit) return
            }
        }

        private fun mostConstrainedCell(): Int {
            var best = -1
            var bestCount = Int.MAX_VALUE
            for (cell in 0 until CELL_COUNT) {
                if (cells[cell] != BLANK) continue
                val used = rows[cell / 9] or columns[cell % 9] or boxes[Geometry.boxIndex(cell)]
                val count = 9 - Integer.bitCount(used)
                if (count < bestCount) {
                    best = cell
                    bestCount = count
                    if (count <= 1) break
                }
            }
            return best
        }

        companion object {
            fun from(cells: List<Int>): Search? {
                require(cells.size == CELL_COUNT)
                val rows = IntArray(9)
                val columns = IntArray(9)
                val boxes = IntArray(9)
                for (cell in 0 until CELL_COUNT) {
                    val digit = cells[cell]
                    if (digit == BLANK) continue
                    val bit = digitBit(digit)
                    val box = Geometry.boxIndex(cell)
                    if ((rows[cell / 9] or columns[cell % 9] or boxes[box]) and bit != 0) return null
                    rows[cell / 9] = rows[cell / 9] or bit
                    columns[cell % 9] = columns[cell % 9] or bit
                    boxes[box] = boxes[box] or bit
                }
                return Search(cells.toIntArray(), rows, columns, boxes)
            }
        }
    }
}
