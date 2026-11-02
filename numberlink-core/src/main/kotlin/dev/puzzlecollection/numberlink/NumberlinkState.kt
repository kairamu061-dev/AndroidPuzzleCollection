package dev.puzzlecollection.numberlink

/**
 * [paths] holds one path per line, starting at whichever endpoint the player began from.
 * Every operation returns a new state, or `this` when ignored. Completion is judged by the
 * rules alone, never by comparing with the generator's solution: see docs/numberlink/logic.
 */
data class NumberlinkState(
    val puzzle: NumberlinkPuzzle,
    val paths: List<List<Int>> = List(puzzle.lines.size) { emptyList() },
    val undoStack: List<List<List<Int>>> = emptyList(),
    val redoStack: List<List<List<Int>>> = emptyList(),
    val active: Int? = null,
    private val strokeBase: List<List<Int>>? = null,
) {
    private val size get() = puzzle.size
    private val endpoints get() = puzzle.endpoints

    /** Line owning [cell] through an endpoint or a path, or null when the cell is empty. */
    fun lineAt(cell: Int): Int? =
        endpointLine(cell) ?: paths.indexOfFirst { cell in it }.takeIf { it >= 0 }

    fun isConnected(line: Int): Boolean {
        val path = paths[line]
        val (a, b) = endpoints[line]
        return path.size >= 2 && setOf(path.first(), path.last()) == setOf(a, b)
    }

    val allConnected: Boolean get() = paths.indices.all(::isConnected)

    val emptyCells: Int get() = (0 until size * size).count { lineAt(it) == null }

    val isSolved: Boolean get() = allConnected && emptyCells == 0

    val canUndo: Boolean get() = undoStack.isNotEmpty() && !isSolved
    val canRedo: Boolean get() = redoStack.isNotEmpty() && !isSolved

    /** Starts at an endpoint (redrawing that line) or inside a path (cutting it there). */
    fun strokeStart(cell: Int): NumberlinkState {
        if (isSolved) return this
        val endpointLine = endpointLine(cell)
        val next = when {
            endpointLine != null -> paths.with(endpointLine, listOf(cell))
            else -> {
                val line = paths.indexOfFirst { cell in it }
                if (line < 0) return this
                paths.with(line, paths[line].subList(0, paths[line].indexOf(cell) + 1))
            }
        }
        val line = endpointLine ?: paths.indexOfFirst { cell in it }
        return copy(paths = next, active = line, strokeBase = paths)
    }

    fun strokeTo(cell: Int): NumberlinkState {
        val line = active ?: return this
        if (isSolved) return this
        val path = paths[line]
        val head = path.last()
        if (cell == head) return this
        val back = path.indexOf(cell)
        if (back >= 0) return copy(paths = paths.with(line, path.subList(0, back + 1)))
        if (isConnected(line)) return this
        if (cell !in neighborsOf(head, size)) return this
        val owner = endpointLine(cell)
        if (owner != null && owner != line) return this
        var next = paths
        val crossed = paths.indexOfFirst { cell in it }
        if (crossed >= 0 && crossed != line) {
            next = next.with(crossed, paths[crossed].subList(0, paths[crossed].indexOf(cell)))
        }
        return copy(paths = next.with(line, path + cell))
    }

    fun strokeEnd(): NumberlinkState {
        val base = strokeBase ?: return this
        val ended = copy(active = null, strokeBase = null)
        return if (base == paths) ended else ended.copy(undoStack = undoStack + listOf(base), redoStack = emptyList())
    }

    fun undo(): NumberlinkState {
        if (!canUndo) return this
        return copy(paths = undoStack.last(), undoStack = undoStack.dropLast(1), redoStack = redoStack + listOf(paths))
    }

    fun redo(): NumberlinkState {
        if (!canRedo) return this
        return copy(paths = redoStack.last(), undoStack = undoStack + listOf(paths), redoStack = redoStack.dropLast(1))
    }

    private fun endpointLine(cell: Int): Int? =
        endpoints.indexOfFirst { (a, b) -> a == cell || b == cell }.takeIf { it >= 0 }
}

private fun List<List<Int>>.with(line: Int, path: List<Int>): List<List<Int>> =
    toMutableList().also { it[line] = path }
