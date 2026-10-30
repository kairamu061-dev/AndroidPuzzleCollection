package dev.puzzlecollection.sudoku

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayTimerTest {
    @Test fun `U-13 counts only while running`() {
        val timer = PlayTimer(1_000)
        assertEquals(1_000, timer.elapsed(50))
        timer.start(100)
        assertEquals(1_500, timer.elapsed(600))
        timer.pause(600)
        assertEquals(1_500, timer.elapsed(9_000))
        timer.start(10_000)
        assertEquals(1_600, timer.elapsed(10_100))
    }

    @Test fun `U-13 repeated calls change nothing`() {
        val timer = PlayTimer()
        timer.start(100)
        timer.start(500)
        assertEquals(500, timer.elapsed(600))
        timer.pause(600)
        timer.pause(900)
        assertEquals(500, timer.elapsed(1_000))
    }
}
