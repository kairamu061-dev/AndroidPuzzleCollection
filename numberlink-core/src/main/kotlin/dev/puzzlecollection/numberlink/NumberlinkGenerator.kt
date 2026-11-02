package dev.puzzlecollection.numberlink

import kotlin.random.Random

/** [lines] is the solution: each line runs from its first endpoint to its second. */
data class NumberlinkPuzzle(val size: Int, val lines: List<List<Int>>) {
    val endpoints: List<Pair<Int, Int>> get() = lines.map { it.first() to it.last() }
}

/** [attempts] counts the covers tried, to tell a slow search from many rejections. */
data class GeneratedNumberlink(val puzzle: NumberlinkPuzzle, val attempts: Int)

object NumberlinkGenerator {
    const val MIN_LINE = 3
    val SIZES = listOf(6, 7, 8)

    /** Same [random] seed, same puzzle. [onAttempt] may throw to cancel; it also runs mid-search. */
    fun generate(size: Int, random: Random, onAttempt: () -> Unit = {}): GeneratedNumberlink {
        var attempts = 0
        while (true) {
            onAttempt()
            attempts++
            val cover = randomCover(size, random) ?: continue
            val lines = makeUnique(size, cover, random, onAttempt) ?: continue
            return GeneratedNumberlink(NumberlinkPuzzle(size, lines), attempts)
        }
    }

    /**
     * Splits lines where another solution departs from [cover] until the solver proves the
     * cover is the only one. Null when a split would leave a line shorter than [MIN_LINE] or the
     * solver runs out of budget.
     */
    internal fun makeUnique(
        size: Int,
        cover: List<List<Int>>,
        random: Random,
        onCheck: () -> Unit = {},
    ): List<List<Int>>? {
        val lines = cover.map { it.toMutableList() }.toMutableList()
        repeat(MAX_SPLITS_PER_CELL * size) {
            val endpoints = lines.map { it.first() to it.last() }
            val solutions = NumberlinkSolver.findSolutions(size, endpoints, onCheck = onCheck) ?: return null
            val intended = edgesOf(lines)
            val other = solutions.firstOrNull { it != intended } ?: return lines
            val departures = (other - intended).shuffled(random)
            val split = departures.firstNotNullOfOrNull { edge ->
                splitAt(lines, (edge shr 32).toInt()) ?: splitAt(lines, edge.toInt())
            } ?: return null
            lines.removeAt(split.first)
            lines.addAll(split.second)
        }
        return null
    }

    /** Cuts the line through [cell] next to it, keeping both pieces at least [MIN_LINE] long. */
    private fun splitAt(lines: List<List<Int>>, cell: Int): Pair<Int, List<MutableList<Int>>>? {
        val index = lines.indexOfFirst { cell in it }
        val line = lines[index]
        val at = line.indexOf(cell)
        val cut = listOf(at, at + 1).firstOrNull { it >= MIN_LINE && line.size - it >= MIN_LINE } ?: return null
        return index to listOf(line.subList(0, cut).toMutableList(), line.subList(cut, line.size).toMutableList())
    }

    private fun edgesOf(lines: List<List<Int>>): Set<Long> = buildSet {
        for (line in lines) line.zipWithNext { a, b -> add(NumberlinkSolver.edgeKey(a, b)) }
    }

    private const val MAX_SPLITS_PER_CELL = 2

    /**
     * Covers the board with lines by joining neighbouring line ends at random.
     * Null when a line shorter than [MIN_LINE] is left: two adjacent endpoints are a free move.
     */
    internal fun randomCover(size: Int, random: Random, maxLength: Int = size * 2): List<List<Int>>? {
        val cellCount = size * size
        val lineOf = IntArray(cellCount) { it }
        val lines = MutableList<ArrayDeque<Int>?>(cellCount) { ArrayDeque(listOf(it)) }
        var merged = true
        while (merged) {
            merged = false
            for (cell in (0 until cellCount).shuffled(random)) {
                val own = lines[lineOf[cell]]!!
                if (own.first() != cell && own.last() != cell) continue
                val partner = neighborsOf(cell, size).filter { other ->
                    val line = lines[lineOf[other]]!!
                    lineOf[other] != lineOf[cell] &&
                        (line.first() == other || line.last() == other) &&
                        own.size + line.size <= maxLength
                }.randomOrNull(random) ?: continue
                join(cell, partner, lineOf, lines)
                merged = true
            }
        }
        val result = lines.filterNotNull().map { it.toList() }
        return if (result.any { it.size < MIN_LINE }) null else result
    }

    private fun join(a: Int, b: Int, lineOf: IntArray, lines: MutableList<ArrayDeque<Int>?>) {
        val first = lines[lineOf[a]]!!
        val second = lines[lineOf[b]]!!
        if (first.last() != a) first.reverse()
        if (second.first() != b) second.reverse()
        val keep = lineOf[a]
        lines[lineOf[b]] = null
        for (cell in second) {
            first.addLast(cell)
            lineOf[cell] = keep
        }
    }
}

private fun <T> ArrayDeque<T>.reverse() {
    val items = toList().asReversed()
    clear()
    addAll(items)
}
