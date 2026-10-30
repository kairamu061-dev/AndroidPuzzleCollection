package dev.puzzlecollection.sudoku

/** [memos] holds a bit set per cell: bit n is digit n. */
data class BoardSnapshot(val values: List<Int>, val memos: List<Int>)

/** Every operation returns a new state, or `this` when the operation is ignored. */
data class GameState(
    val puzzle: Puzzle,
    val board: BoardSnapshot = BoardSnapshot(puzzle.givens, List(CELL_COUNT) { 0 }),
    val selected: Int? = null,
    val memoMode: Boolean = false,
    val undoStack: List<BoardSnapshot> = emptyList(),
    val redoStack: List<BoardSnapshot> = emptyList(),
) {
    val isSolved: Boolean get() = board.values == puzzle.solution

    val canUndo: Boolean get() = undoStack.isNotEmpty() && !isSolved
    val canRedo: Boolean get() = redoStack.isNotEmpty() && !isSolved

    /** Cells that share a row, column, or box with the same digit. */
    val conflicts: Set<Int>
        get() = buildSet {
            for (unit in Geometry.units) {
                val filled = unit.filter { board.values[it] != BLANK }.groupBy { board.values[it] }
                filled.values.filter { it.size > 1 }.forEach(::addAll)
            }
        }

    /** Index 0 is digit 1. Never negative, even with a digit placed more than 9 times. */
    val remaining: List<Int>
        get() = DIGITS.map { digit -> (9 - board.values.count { it == digit }).coerceAtLeast(0) }

    fun isGiven(cell: Int): Boolean = puzzle.givens[cell] != BLANK

    fun hasMemo(cell: Int, digit: Int): Boolean = board.memos[cell] and digitBit(digit) != 0

    fun select(cell: Int): GameState = copy(selected = cell)

    fun toggleMemoMode(): GameState = copy(memoMode = !memoMode)

    fun input(digit: Int): GameState {
        val cell = editableCell() ?: return this
        return when {
            memoMode -> toggleMemo(cell, digit)
            board.values[cell] == digit -> commit(board.withValue(cell, BLANK))
            else -> commit(board.place(cell, digit))
        }
    }

    fun erase(): GameState {
        val cell = editableCell() ?: return this
        if (board.values[cell] == BLANK && board.memos[cell] == 0) return this
        return commit(board.withValue(cell, BLANK).withMemo(cell, 0))
    }

    fun undo(): GameState {
        if (!canUndo) return this
        return copy(
            board = undoStack.last(),
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + board,
        )
    }

    fun redo(): GameState {
        if (!canRedo) return this
        return copy(
            board = redoStack.last(),
            undoStack = undoStack + board,
            redoStack = redoStack.dropLast(1),
        )
    }

    private fun editableCell(): Int? =
        selected?.takeUnless { isSolved || isGiven(it) }

    private fun toggleMemo(cell: Int, digit: Int): GameState {
        if (board.values[cell] != BLANK) return this
        return commit(board.withMemo(cell, board.memos[cell] xor digitBit(digit)))
    }

    private fun commit(next: BoardSnapshot): GameState =
        copy(board = next, undoStack = undoStack + board, redoStack = emptyList())
}

private fun BoardSnapshot.withValue(cell: Int, digit: Int): BoardSnapshot =
    copy(values = values.toMutableList().also { it[cell] = digit })

private fun BoardSnapshot.withMemo(cell: Int, mask: Int): BoardSnapshot =
    copy(memos = memos.toMutableList().also { it[cell] = mask })

/** Placing a digit also retires the memos it makes impossible. */
private fun BoardSnapshot.place(cell: Int, digit: Int): BoardSnapshot {
    val nextMemos = memos.toMutableList()
    nextMemos[cell] = 0
    val keep = digitBit(digit).inv()
    for (peer in Geometry.peers[cell]) nextMemos[peer] = nextMemos[peer] and keep
    return BoardSnapshot(values.toMutableList().also { it[cell] = digit }, nextMemos)
}
