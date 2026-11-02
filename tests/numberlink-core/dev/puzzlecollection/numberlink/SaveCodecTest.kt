package dev.puzzlecollection.numberlink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SaveCodecTest {
    private val state = NumberlinkState(SAMPLE).draw(0, 1, 2).draw(14, 15)
    private val encoded = NumberlinkSaveCodec.encode(state, 4_000)
    private fun replaceLine(i: Int, line: String) =
        encoded.lines().toMutableList().also { it[i] = line }.joinToString("\n")

    @Test fun `U-01 round trip`() {
        val (decoded, elapsed) = NumberlinkSaveCodec.decode(encoded)!!
        assertEquals(state.puzzle, decoded.puzzle)
        assertEquals(state.paths, decoded.paths)
        assertEquals(4_000, elapsed)
    }

    @Test fun `U-02 unknown version`() {
        assertNull(NumberlinkSaveCodec.decode(replaceLine(0, "numberlink-save 2")))
    }

    @Test fun `U-03 damaged data`() {
        assertNotNull(NumberlinkSaveCodec.decode(replaceLine(3, "//")))
        listOf(
            "",
            encoded.lines().take(3).joinToString("\n"),
            replaceLine(3, "0.1.99//"),
            replaceLine(3, "0.2//"),
            replaceLine(3, "1.2//"),
            replaceLine(3, "0.1/4.5.1/"),
            replaceLine(3, "0.4//"),
            replaceLine(2, "0.1.2.3.7/4.5.6.10.11.15.14/8.12.13"),
            replaceLine(4, "-1"),
        ).forEachIndexed { i, text -> assertNull("case $i", NumberlinkSaveCodec.decode(text)) }
    }

    @Test fun `U-04 the format is pinned`() {
        assertEquals(
            "numberlink-save 1\n4\n0.1.2.3.7/4.5.6.10.11.15.14/8.12.13.9\n0.1.2/14.15/\n4000\n",
            encoded,
        )
    }
}
