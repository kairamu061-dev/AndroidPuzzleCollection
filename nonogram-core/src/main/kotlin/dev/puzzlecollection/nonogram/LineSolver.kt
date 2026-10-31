package dev.puzzlecollection.nonogram

/** Solves by per-line deduction only. A full result therefore needs no guessing and is unique. */
object LineSolver {
    internal const val UNKNOWN = -1
    internal const val EMPTY = 0
    internal const val FILLED = 1

    /** Returns the solution, or null when deduction stalls or finds a contradiction. */
    fun solve(size: Int, rowClues: List<List<Int>>, columnClues: List<List<Int>>): List<Boolean>? {
        val grid = IntArray(size * size) { UNKNOWN }
        var changed = true
        while (changed) {
            changed = false
            for (index in 0 until size) {
                val rowCells = IntArray(size) { index * size + it }
                val columnCells = IntArray(size) { it * size + index }
                val rowChanged = refine(grid, rowCells, rowClues[index]) ?: return null
                val columnChanged = refine(grid, columnCells, columnClues[index]) ?: return null
                changed = changed || rowChanged || columnChanged
            }
        }
        return if (grid.any { it == UNKNOWN }) null else grid.map { it == FILLED }
    }

    private fun refine(grid: IntArray, cells: IntArray, clues: List<Int>): Boolean? {
        val line = IntArray(cells.size) { grid[cells[it]] }
        val solved = solveLine(line, clues) ?: return null
        var changed = false
        for (i in cells.indices) {
            if (grid[cells[i]] != solved[i]) {
                grid[cells[i]] = solved[i]
                changed = true
            }
        }
        return changed
    }

    /** Marks every cell that has the same value in all placements consistent with [line]. */
    internal fun solveLine(line: IntArray, clues: List<Int>): IntArray? {
        val blocks = clues.filter { it > 0 }
        val n = line.size
        val m = blocks.size

        // Whether a block of `length` can start at `start` over known cells.
        fun fits(start: Int, length: Int): Boolean =
            start >= 0 && start + length <= n && (start until start + length).none { line[it] == EMPTY }

        // before[i][k]: cells 0 until i can hold exactly blocks 0 until k.
        val before = Array(n + 1) { BooleanArray(m + 1) }
        before[0][0] = true
        for (i in 1..n) {
            for (k in 0..m) {
                val blank = line[i - 1] != FILLED && before[i - 1][k]
                val block = k > 0 && run {
                    val start = i - blocks[k - 1]
                    fits(start, blocks[k - 1]) &&
                        if (start == 0) k == 1 else line[start - 1] != FILLED && before[start - 1][k - 1]
                }
                before[i][k] = blank || block
            }
        }

        // after[i][k]: cells i until n can hold exactly blocks k until m.
        val after = Array(n + 2) { BooleanArray(m + 1) }
        after[n][m] = true
        for (i in n - 1 downTo 0) {
            for (k in 0..m) {
                val blank = line[i] != FILLED && after[i + 1][k]
                val block = k < m && fits(i, blocks[k]) && run {
                    val end = i + blocks[k]
                    if (end == n) k == m - 1 else line[end] != FILLED && after[end + 1][k + 1]
                }
                after[i][k] = blank || block
            }
        }
        if (!before[n][m]) return null

        val canEmpty = BooleanArray(n) { i ->
            line[i] != FILLED && (0..m).any { k -> before[i][k] && after[i + 1][k] }
        }
        val fillDelta = IntArray(n + 1)
        for (k in 0 until m) {
            val length = blocks[k]
            for (start in 0..n - length) {
                if (!fits(start, length)) continue
                val left = if (start == 0) k == 0 else line[start - 1] != FILLED && before[start - 1][k]
                val end = start + length
                val right = if (end == n) k == m - 1 else line[end] != FILLED && after[end + 1][k + 1]
                if (left && right) {
                    fillDelta[start]++
                    fillDelta[end]--
                }
            }
        }
        var active = 0
        return IntArray(n) { i ->
            active += fillDelta[i]
            when {
                active > 0 && canEmpty[i] -> UNKNOWN
                active > 0 -> FILLED
                else -> EMPTY
            }
        }
    }
}
