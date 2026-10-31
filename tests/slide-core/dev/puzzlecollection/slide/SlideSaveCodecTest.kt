package dev.puzzlecollection.slide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SlideSaveCodecTest {
    private val state = SlideState(3, listOf(1, 2, 3, 4, 0, 5, 7, 8, 6), moves = 12)
    private val encoded = SlideSaveCodec.encode(state, 3_000)
    private fun replaceLine(i: Int, line: String) =
        encoded.lines().toMutableList().also { it[i] = line }.joinToString("\n")

    @Test fun `U-01 round trip`() {
        assertEquals(state to 3_000L, SlideSaveCodec.decode(encoded))
    }

    @Test fun `U-02 unknown version`() {
        assertNull(SlideSaveCodec.decode(replaceLine(0, "slide-save 2")))
    }

    @Test fun `U-03 damaged data`() {
        assertNotNull(SlideSaveCodec.decode(replaceLine(3, "12")))
        listOf(
            "",
            encoded.lines().take(3).joinToString("\n"),
            replaceLine(1, "2"),
            replaceLine(1, "6"),
            replaceLine(2, "1,1,3,4,0,5,7,8,6"),
            replaceLine(2, "1,2,3"),
            replaceLine(3, "-1"),
            replaceLine(4, "x"),
        ).forEachIndexed { i, text -> assertNull("case $i", SlideSaveCodec.decode(text)) }
    }

    @Test fun `U-04 the format is pinned`() {
        assertEquals("slide-save 1\n3\n1,2,3,4,0,5,7,8,6\n12\n3000\n", encoded)
    }
}
