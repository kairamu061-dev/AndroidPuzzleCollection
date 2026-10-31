package dev.puzzlecollection.nonogram

enum class Mark { EMPTY, FILLED, CROSS }

/** A stroke is one press-to-release gesture: it is one undo step and applies one change. */
data class NonogramState(
    val puzzle: NonogramPuzzle,
    val pictureId: String? = null,
    val cells: List<Mark> = List(puzzle.size * puzzle.size) { Mark.EMPTY },
    val tool: Mark = Mark.FILLED,
    val focus: Int? = null,
    val undoStack: List<List<Mark>> = emptyList(),
    val redoStack: List<List<Mark>> = emptyList(),
    private val strokeBase: List<Mark>? = null,
    private val strokeTarget: Mark? = null,
) {
    val isSolved: Boolean
        get() = cells.indices.all { (cells[it] == Mark.FILLED) == puzzle.solution[it] }

    val canUndo: Boolean get() = undoStack.isNotEmpty() && !isSolved
    val canRedo: Boolean get() = redoStack.isNotEmpty() && !isSolved

    fun isRowDone(index: Int): Boolean =
        cluesOf(row(cells, puzzle.size, index).map { it == Mark.FILLED }) == puzzle.rowClues[index]

    fun isColumnDone(index: Int): Boolean =
        cluesOf(column(cells, puzzle.size, index).map { it == Mark.FILLED }) == puzzle.columnClues[index]

    fun selectTool(next: Mark): NonogramState = copy(tool = next)

    /** The first cell decides the stroke: repainting a cell with the tool erases instead. */
    fun strokeStart(cell: Int): NonogramState {
        if (isSolved) return this
        val target = if (cells[cell] == tool) Mark.EMPTY else tool
        return copy(
            cells = cells.with(cell, target),
            focus = cell,
            strokeBase = cells,
            strokeTarget = target,
        )
    }

    /** Later cells only change what the stroke's intent covers, so a drag cannot wipe marks. */
    fun strokeTo(cell: Int): NonogramState {
        val target = strokeTarget ?: return this
        if (isSolved) return this
        val expected = if (target == Mark.EMPTY) tool else Mark.EMPTY
        val next = if (cells[cell] == expected) cells.with(cell, target) else cells
        return copy(cells = next, focus = cell)
    }

    fun strokeEnd(): NonogramState {
        val base = strokeBase ?: return this
        val ended = copy(strokeBase = null, strokeTarget = null)
        return if (base == cells) ended else ended.copy(undoStack = undoStack + listOf(base), redoStack = emptyList())
    }

    /** Drops the stroke in progress: the first finger of a pinch must leave no mark. */
    fun strokeCancel(): NonogramState {
        val base = strokeBase ?: return this
        if (isSolved) return strokeEnd()
        return copy(cells = base, strokeBase = null, strokeTarget = null)
    }

    fun undo(): NonogramState {
        if (!canUndo) return this
        return copy(
            cells = undoStack.last(),
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + listOf(cells),
        )
    }

    fun redo(): NonogramState {
        if (!canRedo) return this
        return copy(
            cells = redoStack.last(),
            undoStack = undoStack + listOf(cells),
            redoStack = redoStack.dropLast(1),
        )
    }
}

private fun List<Mark>.with(cell: Int, mark: Mark): List<Mark> =
    toMutableList().also { it[cell] = mark }
