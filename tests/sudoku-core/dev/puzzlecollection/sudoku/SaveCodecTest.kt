package dev.puzzlecollection.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveCodecTest {
    private val played = GameState(SAMPLE_PUZZLE).select(R0C2).input(4)
        .select(R0C3).toggleMemoMode().input(1).input(9)
    private val game = SavedGame(played, 12_345)
    private val encoded = SaveCodec.encode(game)

    private fun replaceLine(index: Int, line: String) =
        encoded.lines().toMutableList().also { it[index] = line }.joinToString("\n")

    @Test fun `U-01 round trip keeps the board, puzzle and time`() {
        val decoded = SaveCodec.decode(encoded)!!
        assertEquals(played.board, decoded.state.board)
        assertEquals(played.puzzle, decoded.state.puzzle)
        assertEquals(12_345, decoded.elapsedMillis)
    }

    @Test fun `U-01 session state starts fresh`() {
        val state = SaveCodec.decode(encoded)!!.state
        assertNull(state.selected)
        assertFalse(state.memoMode)
        assertTrue(state.undoStack.isEmpty())
    }

    @Test fun `U-02 an unknown version is rejected`() {
        assertNull(SaveCodec.decode(replaceLine(0, "sudoku-save 2")))
    }

    @Test fun `U-03 the helper keeps a valid save valid`() {
        assertNotNull(SaveCodec.decode(replaceLine(1, "EASY")))
    }

    @Test fun `U-03 damaged data is rejected`() {
        val damaged = listOf(
            "",
            encoded.lines().take(4).joinToString("\n"),
            replaceLine(1, "IMPOSSIBLE"),
            replaceLine(2, "123"),
            replaceLine(2, "x".repeat(CELL_COUNT)),
            replaceLine(3, "0".repeat(CELL_COUNT)),
            replaceLine(3, "1".repeat(CELL_COUNT)),
            replaceLine(4, encoded.lines()[4].replaceFirst('5', '6')),
            replaceLine(5, "1,2,3"),
            replaceLine(5, List(CELL_COUNT) { "1" }.joinToString(",")),
            replaceLine(5, List(CELL_COUNT) { "x" }.joinToString(",")),
            replaceLine(6, "-1"),
            replaceLine(6, "soon"),
        )
        damaged.forEachIndexed { i, text -> assertNull("case $i", SaveCodec.decode(text)) }
    }

    @Test fun `U-04 the format is pinned`() {
        val memos = MutableList(CELL_COUNT) { 0 }.also { it[R0C3] = 2 or 512 }.joinToString(",")
        val expected = """
            sudoku-save 1
            EASY
            530070000600195000098000060800060003400803001700020006060000280000419005000080079
            534678912672195348198342567859761423426853791713924856961537284287419635345286179
            534070000600195000098000060800060003400803001700020006060000280000419005000080079
            $memos
            12345
        """.trimIndent() + "\n"
        assertEquals(expected, encoded)
    }

    @Test fun `U-05 only board changes need saving`() {
        val state = GameState(SAMPLE_PUZZLE)
        assertTrue(shouldSave(state, state.select(R0C2).input(4)))
        assertFalse(shouldSave(state, state.select(R0C2)))
        assertFalse(shouldSave(state, state.toggleMemoMode()))
    }
}
