package dev.puzzlecollection.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramPicturesTest {
    private val pictures = NonogramPictures.all

    @Test fun `U-01 the bundled data loads`() {
        assertEquals(100, pictures.size)
        pictures.forEach {
            assertEquals(PICTURE_SIZE, it.puzzle.size)
            assertEquals(it.id, PICTURE_SIZE * PICTURE_SIZE, it.puzzle.solution.size)
        }
    }

    @Test fun `U-02 ids are unique and names are present`() {
        assertEquals(pictures.size, pictures.map { it.id }.toSet().size)
        assertTrue(pictures.all { it.name.isNotBlank() })
    }

    @Test fun `U-03 every picture solves by deduction alone`() {
        pictures.forEach {
            val puzzle = it.puzzle
            assertEquals(it.id, puzzle.solution, LineSolver.solve(puzzle.size, puzzle.rowClues, puzzle.columnClues))
        }
    }

    @Test fun `U-04 the next picture is the first unfinished one`() {
        assertEquals(pictures[0], nextPicture(pictures, emptySet()))
        assertEquals(pictures[2], nextPicture(pictures, setOf(pictures[0].id, pictures[1].id)))
        assertNull(nextPicture(pictures, pictures.map { it.id }.toSet()))
    }

    @Test fun `U-05 progress round trip keeps unknown ids`() {
        val completed = setOf("rabbit", "gone-picture")
        assertEquals(completed, NonogramProgress.decode(NonogramProgress.encode(completed)))
        assertTrue(NonogramProgress.decode("").isEmpty())
        assertFalse("rabbit" in NonogramProgress.decode("cat\n"))
    }
}
