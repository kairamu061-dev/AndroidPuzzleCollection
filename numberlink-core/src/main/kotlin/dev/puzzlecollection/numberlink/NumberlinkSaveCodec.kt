package dev.puzzlecollection.numberlink

/** Format: docs/numberlink/save/design.md. Bump [VERSION] whenever the layout changes. */
object NumberlinkSaveCodec {
    private const val VERSION = 1
    private const val HEADER = "numberlink-save $VERSION"
    private const val LINE_COUNT = 5

    fun encode(state: NumberlinkState, elapsedMillis: Long): String = listOf(
        HEADER,
        state.puzzle.size.toString(),
        encodePaths(state.puzzle.lines),
        encodePaths(state.paths),
        elapsedMillis.toString(),
    ).joinToString("\n", postfix = "\n")

    fun decode(text: String): Pair<NumberlinkState, Long>? {
        val lines = text.removeSuffix("\n").split("\n")
        if (lines.size != LINE_COUNT || lines[0] != HEADER) return null
        val size = lines[1].toIntOrNull()?.takeIf { it in 2..MAX_SIZE } ?: return null
        val solution = decodePaths(lines[2], size) ?: return null
        val paths = decodePaths(lines[3], size) ?: return null
        val elapsed = lines[4].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        if (solution.any { it.size < 2 } || paths.size != solution.size) return null
        if (solution.flatten().sorted() != (0 until size * size).toList()) return null
        val puzzle = NumberlinkPuzzle(size, solution)
        val endpoints = puzzle.endpoints
        val used = mutableSetOf<Int>()
        for ((line, path) in paths.withIndex()) {
            if (path.isEmpty()) continue
            val (a, b) = endpoints[line]
            if (path.first() != a && path.first() != b) return null
            if (path.any { !used.add(it) }) return null
            val others = endpoints.filterIndexed { i, _ -> i != line }.flatMap { listOf(it.first, it.second) }
            if (path.any { it in others }) return null
        }
        return NumberlinkState(puzzle, paths) to elapsed
    }

    private fun encodePaths(paths: List<List<Int>>): String =
        paths.joinToString("/") { it.joinToString(".") }

    /** Null unless every path steps between orthogonal neighbours inside the board. */
    private fun decodePaths(text: String, size: Int): List<List<Int>>? = text.split("/").map { part ->
        if (part.isEmpty()) return@map emptyList()
        val cells = part.split(".").map { it.toIntOrNull()?.takeIf { cell -> cell in 0 until size * size } ?: return null }
        if (cells.zipWithNext().any { (a, b) -> b !in neighborsOf(a, size) }) return null
        cells
    }

    private const val MAX_SIZE = 12
}
