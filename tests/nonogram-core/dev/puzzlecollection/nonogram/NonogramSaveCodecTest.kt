package dev.puzzlecollection.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NonogramSaveCodecTest {
    private val state = NonogramState(HEART, HEART.solution.indices.map {
        when (it) { 1 -> Mark.FILLED; 0 -> Mark.CROSS; else -> Mark.EMPTY }
    })
    private val encoded = NonogramSaveCodec.encode(state, 7_000)
    private fun replaceLine(i: Int, line: String) =
        encoded.lines().toMutableList().also { it[i] = line }.joinToString("\n")

    @Test fun `U-01 round trip`() {
        val (decoded, elapsed) = NonogramSaveCodec.decode(encoded)!!
        assertEquals(state.puzzle.solution, decoded.puzzle.solution)
        assertEquals(state.cells, decoded.cells)
        assertEquals(7_000, elapsed)
    }

    @Test fun `U-02 unknown version`() {
        assertNull(NonogramSaveCodec.decode(replaceLine(0, "nonogram-save 2")))
    }

    @Test fun `U-03 damaged data`() {
        assertNotNull(NonogramSaveCodec.decode(replaceLine(4, "1")))
        listOf(
            "",
            encoded.lines().take(3).joinToString("\n"),
            replaceLine(1, "7"),
            replaceLine(2, "010"),
            replaceLine(2, "2".repeat(25)),
            replaceLine(3, "3".repeat(25)),
            replaceLine(4, "-1"),
        ).forEachIndexed { i, text -> assertNull("case $i", NonogramSaveCodec.decode(text)) }
    }

    @Test fun `U-04 the format is pinned`() {
        assertEquals(
            "nonogram-save 1\n5\n0101011111111110111000100\n2100000000000000000000000\n7000\n",
            encoded,
        )
    }
}
