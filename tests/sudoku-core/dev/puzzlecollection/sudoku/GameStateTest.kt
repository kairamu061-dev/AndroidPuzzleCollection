package dev.puzzlecollection.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {
    private val fresh = GameState(SAMPLE_PUZZLE)

    private fun GameState.value(cell: Int) = board.values[cell]
    private fun GameState.memo(cell: Int, digit: Int) = select(cell).toggleMemoMode().input(digit).toggleMemoMode()

    private fun solvedExcept(vararg blank: Int): GameState {
        val values = SAMPLE_SOLUTION.toMutableList()
        blank.forEach { values[it] = BLANK }
        return GameState(SAMPLE_PUZZLE, BoardSnapshot(values, List(CELL_COUNT) { 0 }))
    }

    @Test fun `U-01 input fills an empty cell and records history`() {
        val next = fresh.select(R0C2).input(4)
        assertEquals(4, next.value(R0C2))
        assertEquals(1, next.undoStack.size)
    }

    @Test fun `U-02 given cells are protected`() {
        val onGiven = fresh.select(R8C8)
        assertSame(onGiven, onGiven.input(1))
        assertSame(onGiven, onGiven.erase())
        val memo = onGiven.toggleMemoMode()
        assertSame(memo, memo.input(1))
    }

    @Test fun `U-03 the same digit clears the cell`() {
        val next = fresh.select(R0C2).input(4).input(4)
        assertEquals(BLANK, next.value(R0C2))
    }

    @Test fun `U-04 memos toggle`() {
        val memo = fresh.select(R0C2).toggleMemoMode()
        assertTrue(memo.input(4).hasMemo(R0C2, 4))
        assertFalse(memo.input(4).input(4).hasMemo(R0C2, 4))
        assertTrue(memo.input(4).input(1).hasMemo(R0C2, 4))
    }

    @Test fun `U-05 no memo on a filled cell`() {
        val filled = fresh.select(R0C2).input(4).toggleMemoMode()
        assertSame(filled, filled.input(1))
    }

    @Test fun `U-06 placing retires memos in peers only`() {
        val before = fresh.memo(R0C3, 4).memo(R1C1, 4).memo(R4C2, 4).memo(FAR_CELL, 4).memo(R0C3, 1)
        val after = before.select(R0C2).input(4)
        assertFalse(after.hasMemo(R0C3, 4))
        assertFalse(after.hasMemo(R1C1, 4))
        assertFalse(after.hasMemo(R4C2, 4))
        assertTrue(after.hasMemo(FAR_CELL, 4))
        assertTrue(after.hasMemo(R0C3, 1))
    }

    @Test fun `U-06 placing clears the cell's own memos`() {
        val after = fresh.memo(R0C2, 1).memo(R0C2, 2).select(R0C2).input(4)
        assertEquals(0, after.board.memos[R0C2])
    }

    @Test fun `U-07 erase clears digits and memos`() {
        assertEquals(BLANK, fresh.select(R0C2).input(4).erase().value(R0C2))
        assertEquals(0, fresh.memo(R0C2, 1).erase().board.memos[R0C2])
    }

    @Test fun `U-07 erasing nothing adds no history`() {
        val empty = fresh.select(R0C2)
        assertSame(empty, empty.erase())
    }

    @Test fun `U-08 undo restores retired memos and redo reapplies`() {
        val before = fresh.memo(R0C3, 4)
        val placed = before.select(R0C2).input(4)
        val undone = placed.undo()
        assertEquals(before.board, undone.board)
        assertEquals(placed.board, undone.redo().board)
    }

    @Test fun `U-08 a new edit clears redo`() {
        val undone = fresh.select(R0C2).input(4).undo()
        assertTrue(undone.canRedo)
        assertFalse(undone.input(1).canRedo)
    }

    @Test fun `U-08 nothing to undo or redo is ignored`() {
        assertSame(fresh, fresh.undo())
        assertSame(fresh, fresh.redo())
    }

    @Test fun `U-09 no duplicate, no conflict`() {
        assertEquals(emptySet<Int>(), fresh.select(R0C2).input(4).conflicts)
    }

    @Test fun `U-09 row only`() {
        assertEquals(setOf(0, R0C2), fresh.select(R0C2).input(5).conflicts)
    }

    @Test fun `U-09 column only`() {
        val rowSevenColumnTwo = 65
        assertEquals(setOf(20, rowSevenColumnTwo), fresh.select(rowSevenColumnTwo).input(8).conflicts)
    }

    @Test fun `U-09 box only`() {
        assertEquals(setOf(R1C1, 20), fresh.select(R1C1).input(8).conflicts)
    }

    @Test fun `U-10 remaining counts`() {
        assertEquals(SAMPLE_GIVENS.count { it == 5 }, 9 - fresh.remaining[4])
        val more = solvedExcept(R0C2).select(R0C2).input(1)
        assertEquals(0, more.remaining[0])
        assertEquals(1, more.remaining[3])
    }

    @Test fun `U-11 solved only when every cell matches`() {
        assertTrue(solvedExcept(R0C2).select(R0C2).input(4).isSolved)
        assertFalse(solvedExcept(R0C2).isSolved)
        assertFalse(solvedExcept(R0C2).select(R0C2).input(1).isSolved)
    }

    @Test fun `U-12 a solved board ignores edits`() {
        val solved = solvedExcept(R0C2).select(R0C2).input(4)
        assertSame(solved, solved.input(1))
        assertSame(solved, solved.erase())
        assertSame(solved, solved.undo())
    }

    @Test fun `U-14 no selection ignores edits`() {
        assertSame(fresh, fresh.input(1))
        assertSame(fresh, fresh.erase())
    }

    @Test fun `U-15 a wrong digit without a conflict is accepted`() {
        val wrong = fresh.select(R0C2).input(1)
        assertEquals(1, wrong.value(R0C2))
        assertEquals(emptySet<Int>(), wrong.conflicts)
    }
}
