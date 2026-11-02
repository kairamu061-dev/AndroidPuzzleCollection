package dev.puzzlecollection.numberlink

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class GeneratorTest {
    @Test fun `U-05 puzzles are unique, fully covered, with no trivial lines`() {
        generated.forEach { (label, result) ->
            val puzzle = result.puzzle
            assertEquals(label, 1, NumberlinkSolver.countSolutions(puzzle.size, puzzle.endpoints))
            assertEquals(label, (0 until puzzle.size * puzzle.size).toList(), puzzle.lines.flatten().sorted())
            assertTrue(label, puzzle.lines.all { it.size >= NumberlinkGenerator.MIN_LINE })
            puzzle.lines.forEach { line ->
                assertTrue(label, line.zipWithNext().all { (a, b) -> b in neighborsOf(a, puzzle.size) })
            }
        }
    }

    @Test fun `U-05 the solution satisfies the play rules`() {
        generated.values.forEach { result ->
            val state = NumberlinkState(result.puzzle, result.puzzle.lines)
            assertTrue(state.isSolved)
        }
    }

    @Test fun `U-03 pruning agrees with brute force on generator covers`() {
        val random = Random(9)
        var unique = 0
        repeat(150) {
            val cover = NumberlinkGenerator.randomCover(5, random) ?: return@repeat
            val endpoints = cover.map { it.first() to it.last() }
            val brute = BruteForceCounter.count(5, endpoints)
            assertEquals(endpoints.toString(), brute, NumberlinkSolver.countSolutions(5, endpoints))
            if (brute == 1) unique++
        }
        println("U-03 covers unique=$unique")
    }

    @Test fun `U-06 seeds reproduce`() {
        assertEquals(NumberlinkGenerator.generate(7, Random(3)), NumberlinkGenerator.generate(7, Random(3)))
        assertNotEquals(NumberlinkGenerator.generate(7, Random(3)), NumberlinkGenerator.generate(7, Random(4)))
    }

    @Test fun `U-07 generation stays under the limit`() {
        durations.forEach { (size, millis) ->
            println("U-07 size=$size max=${millis.max()}ms avg=${millis.average().toInt()}ms")
            assertTrue("$size took ${millis.max()} ms", millis.max() < 5_000)
        }
    }

    companion object {
        private val generated = mutableMapOf<String, GeneratedNumberlink>()
        private val durations = mutableMapOf<Int, MutableList<Long>>()

        @BeforeClass @JvmStatic fun generateAll() {
            for (size in NumberlinkGenerator.SIZES) for (seed in 1L..30) {
                val start = System.nanoTime()
                generated["size=$size seed=$seed"] = NumberlinkGenerator.generate(size, Random(seed))
                durations.getOrPut(size) { mutableListOf() } += (System.nanoTime() - start) / 1_000_000
            }
        }
    }
}
