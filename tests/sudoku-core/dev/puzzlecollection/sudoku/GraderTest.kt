package dev.puzzlecollection.sudoku

import org.junit.Assert.assertEquals
import org.junit.Test

class GraderTest {
    @Test fun `U-07 singles only is easy`() {
        assertEquals(Difficulty.EASY, Grader.grade(SAMPLE_GIVENS))
    }

    /** Pinned from generator seed 1; singles alone stall on it, checked by a separate solver. */
    @Test fun `U-07 a puzzle singles cannot finish is medium`() {
        val medium = digits(
            "000406000 410080902 300000016 700034050 000000000 050120003 290000008 107050029 000209000",
        )
        assertEquals(1, Solver.countSolutions(medium))
        assertEquals(Difficulty.MEDIUM, Grader.grade(medium))
    }

    @Test fun `U-07 AI Escargot is hard`() {
        val aiEscargot = digits(
            "100007090 030020008 009600500 005300900 010080002 600004000 300000010 040000007 007000300",
        )
        assertEquals(1, Solver.countSolutions(aiEscargot))
        assertEquals(Difficulty.HARD, Grader.grade(aiEscargot))
    }
}
