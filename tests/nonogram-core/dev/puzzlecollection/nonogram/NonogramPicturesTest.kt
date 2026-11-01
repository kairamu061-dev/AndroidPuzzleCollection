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

    /** Symmetry lets a player guess half the board. Definition: docs/nonogram/pictures/spec.md. */
    @Test fun `U-06 at most 10 percent of the pictures are symmetric`() {
        val symmetric = pictures.filter { symmetryOf(it.puzzle) >= 0.95 }
        assertTrue(symmetric.map { it.id }.toString(), symmetric.size * 10 <= pictures.size)
    }

    @Test fun `U-06 the symmetry measure tells the two kinds apart`() {
        assertEquals(1.0, symmetryOf(HEART), 0.001)
        val lopsided = NonogramPuzzle(5, ("###.." + "##..." + "#...." + "#...." + "#....").map { it == '#' })
        assertTrue(symmetryOf(lopsided) < 0.95)
    }

    private fun symmetryOf(puzzle: NonogramPuzzle): Double {
        val size = puzzle.size
        val filled = puzzle.solution.count { it }
        return (-1..1).maxOf { shift ->
            puzzle.solution.indices.count { cell ->
                val mirror = size - 1 - cell % size + shift
                puzzle.solution[cell] && mirror in 0 until size &&
                    puzzle.solution[cell / size * size + mirror]
            }.toDouble() / filled
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
