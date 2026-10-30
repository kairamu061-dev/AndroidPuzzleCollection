package dev.puzzlecollection.sudoku

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test fun `U-01 elapsed time boundaries`() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("00:59", formatElapsed(59_999))
        assertEquals("01:00", formatElapsed(60_000))
        assertEquals("59:59", formatElapsed(3_599_999))
        assertEquals("1:00:00", formatElapsed(3_600_000))
    }
}
