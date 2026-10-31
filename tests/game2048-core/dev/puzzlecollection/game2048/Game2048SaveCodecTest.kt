package dev.puzzlecollection.game2048

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Game2048SaveCodecTest {
    private val state = board(2, 0, 4, 0, 0, 0, 0, 2048, *IntArray(8), score = 20, best = 100, reached = true)
    private val encoded = Game2048SaveCodec.encode(state)
    private fun replaceLine(i: Int, line: String) =
        encoded.lines().toMutableList().also { it[i] = line }.joinToString("\n")

    @Test fun `U-01 round trip`() {
        val decoded = Game2048SaveCodec.decode(encoded)!!
        assertEquals(state.values, decoded.values)
        assertEquals(20, decoded.score)
        assertEquals(100, decoded.best)
        assertTrue(decoded.reached2048)
        assertEquals(decoded.tiles.size, decoded.tiles.map { it.id }.toSet().size)
    }

    @Test fun `U-02 unknown version`() {
        assertNull(Game2048SaveCodec.decode(replaceLine(0, "2048-save 2")))
    }

    @Test fun `U-03 damaged data`() {
        assertNotNull(Game2048SaveCodec.decode(replaceLine(2, "21")))
        listOf(
            "",
            replaceLine(1, "2,4"),
            replaceLine(1, List(16) { "3" }.joinToString(",")),
            replaceLine(1, List(16) { "1" }.joinToString(",")),
            replaceLine(2, "-1"),
            replaceLine(2, "101"),
            replaceLine(4, "2"),
        ).forEachIndexed { i, text -> assertNull("case $i", Game2048SaveCodec.decode(text)) }
    }

    @Test fun `U-04 the format is pinned`() {
        assertEquals("2048-save 1\n2,0,4,0,0,0,0,2048,0,0,0,0,0,0,0,0\n20\n100\n1\n", encoded)
    }
}
