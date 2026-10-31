package dev.puzzlecollection.nonogram

/** Format: docs/nonogram/save/design.md. Bump [VERSION] whenever the layout changes. */
object NonogramSaveCodec {
    private const val VERSION = 1
    private const val HEADER = "nonogram-save $VERSION"
    private const val LINE_COUNT = 5

    fun encode(state: NonogramState, elapsedMillis: Long): String = listOf(
        HEADER,
        state.puzzle.size.toString(),
        state.puzzle.solution.joinToString("") { if (it) "1" else "0" },
        state.cells.joinToString("") { it.ordinal.toString() },
        elapsedMillis.toString(),
    ).joinToString("\n", postfix = "\n")

    fun decode(text: String): Pair<NonogramState, Long>? {
        val lines = text.removeSuffix("\n").split("\n")
        if (lines.size != LINE_COUNT || lines[0] != HEADER) return null
        val size = lines[1].toIntOrNull()?.takeIf { it in NONOGRAM_SIZES } ?: return null
        val cellCount = size * size
        if (lines[2].length != cellCount || !lines[2].all { it in "01" }) return null
        if (lines[3].length != cellCount || !lines[3].all { it in "012" }) return null
        val elapsed = lines[4].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val puzzle = NonogramPuzzle(size, lines[2].map { it == '1' })
        val cells = lines[3].map { Mark.entries[it - '0'] }
        return NonogramState(puzzle, cells) to elapsed
    }
}
