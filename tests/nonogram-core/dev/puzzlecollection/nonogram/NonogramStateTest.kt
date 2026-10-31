package dev.puzzlecollection.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramStateTest {
    private val fresh = NonogramState(HEART)
    private fun NonogramState.paint(vararg cells: Int): NonogramState {
        var state = strokeStart(cells.first())
        cells.drop(1).forEach { state = state.strokeTo(it) }
        return state.strokeEnd()
    }

    private fun solved() = HEART.solution.indices.filter { HEART.solution[it] }
        .fold(fresh) { state, cell -> state.paint(cell) }

    @Test fun `U-08 a stroke from an empty cell fills`() {
        val state = fresh.paint(0, 1, 2)
        assertEquals(List(3) { Mark.FILLED }, state.cells.take(3))
    }

    @Test fun `U-08 a stroke from a filled cell erases`() {
        val state = fresh.paint(0, 1, 2).paint(0, 1)
        assertEquals(listOf(Mark.EMPTY, Mark.EMPTY, Mark.FILLED), state.cells.take(3))
    }

    @Test fun `U-08 a fill stroke does not overwrite crosses`() {
        val state = fresh.selectTool(Mark.CROSS).paint(1).selectTool(Mark.FILLED).paint(0, 1, 2)
        assertEquals(listOf(Mark.FILLED, Mark.CROSS, Mark.FILLED), state.cells.take(3))
    }

    @Test fun `U-14 a cancelled stroke leaves no mark and no history`() {
        val state = fresh.paint(4).strokeStart(0).strokeTo(1).strokeCancel()
        assertEquals(fresh.paint(4).cells, state.cells)
        assertEquals(1, state.undoStack.size)
        assertEquals(state, state.strokeTo(2))
    }

    @Test fun `U-09 one stroke is one undo`() {
        val state = fresh.paint(0, 1, 2)
        assertEquals(1, state.undoStack.size)
        assertEquals(fresh.cells, state.undo().cells)
    }

    @Test fun `U-10 undo, redo, and a new stroke clearing redo`() {
        val undone = fresh.paint(0).undo()
        assertEquals(Mark.FILLED, undone.redo().cells[0])
        assertFalse(undone.paint(4).canRedo)
    }

    @Test fun `U-10 a stroke changing nothing adds no history`() {
        val state = fresh.paint(0).paint(0).undo().undo().undo()
        assertEquals(fresh.cells, state.cells)
        val crossed = fresh.selectTool(Mark.CROSS).paint(0)
        val unchanged = crossed.strokeEnd()
        assertEquals(crossed.undoStack.size, unchanged.undoStack.size)
    }

    @Test fun `a picture keeps its id through play`() {
        assertEquals("heart", NonogramState(HEART, pictureId = "heart").strokeStart(0).strokeEnd().pictureId)
    }

    @Test fun `U-11 solved with crosses, not with an extra fill`() {
        val done = solved()
        assertTrue(done.isSolved)
        val emptyCell = HEART.solution.indexOf(false)
        val partial = fresh.selectTool(Mark.CROSS).paint(emptyCell).selectTool(Mark.FILLED)
        val withCross = HEART.solution.indices.filter { HEART.solution[it] }.fold(partial) { s, c -> s.paint(c) }
        assertTrue(withCross.isSolved)
        val lastFilled = HEART.solution.lastIndexOf(true)
        val almost = HEART.solution.indices.filter { HEART.solution[it] && it != lastFilled }
            .fold(fresh) { s, c -> s.paint(c) }.paint(emptyCell)
        assertFalse(almost.paint(lastFilled).isSolved)
    }

    @Test fun `U-12 a solved board ignores edits`() {
        val done = solved()
        assertSame(done, done.strokeStart(0))
        assertSame(done, done.undo())
    }

    @Test fun `U-13 line completion`() {
        val row = fresh.paint(1, 3)
        assertTrue(row.isRowDone(0))
        assertFalse(row.isRowDone(1))
        assertFalse(fresh.paint(1).isRowDone(0))
        assertTrue(fresh.paint(22).isRowDone(4))
    }
}
