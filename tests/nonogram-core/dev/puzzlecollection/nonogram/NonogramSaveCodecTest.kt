package dev.puzzlecollection.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NonogramSaveCodecTest {
    private val state = NonogramState(
        HEART,
        pictureId = "heart",
        cells = HEART.solution.indices.map {
            when (it) { 1 -> Mark.FILLED; 0 -> Mark.CROSS; else -> Mark.EMPTY }
        },
    )
    private val encoded = NonogramSaveCodec.encode(state, 7_000)
    private fun replaceLine(i: Int, line: String) =
        encoded.lines().toMutableList().also { it[i] = line }.joinToString("\n")

    @Test fun `U-01 round trip`() {
        val (decoded, elapsed) = NonogramSaveCodec.decode(encoded)!!
        assertEquals(state.puzzle.solution, decoded.puzzle.solution)
        assertEquals(state.cells, decoded.cells)
        assertEquals("heart", decoded.pictureId)
        assertEquals(7_000, elapsed)
    }

    @Test fun `U-01 a game without a picture round trips`() {
        val plain = NonogramSaveCodec.encode(NonogramState(HEART), 0)
        assertNull(NonogramSaveCodec.decode(plain)!!.first.pictureId)
    }

    @Test fun `U-02 unknown version`() {
        assertNull(NonogramSaveCodec.decode(replaceLine(0, "nonogram-save 3")))
    }

    @Test fun `U-03 damaged data`() {
        assertNotNull(NonogramSaveCodec.decode(replaceLine(5, "1")))
        listOf(
            "",
            encoded.lines().take(3).joinToString("\n"),
            replaceLine(1, "7"),
            replaceLine(3, "010"),
            replaceLine(3, "2".repeat(25)),
            replaceLine(4, "3".repeat(25)),
            replaceLine(5, "-1"),
        ).forEachIndexed { i, text -> assertNull("case $i", NonogramSaveCodec.decode(text)) }
    }

    @Test fun `U-04 the format is pinned`() {
        assertEquals(
            "nonogram-save 2\n5\nheart\n0101011111111110111000100\n2100000000000000000000000\n7000\n",
            encoded,
        )
    }

    @Test fun `U-05 version 1 saves still load`() {
        val v1 = "nonogram-save 1\n5\n0101011111111110111000100\n2100000000000000000000000\n7000\n"
        val (decoded, elapsed) = NonogramSaveCodec.decode(v1)!!
        assertNull(decoded.pictureId)
        assertEquals(Mark.FILLED, decoded.cells[1])
        assertEquals(7_000, elapsed)
    }
}
