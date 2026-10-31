package dev.puzzlecollection.nonogram

/** Format: docs/nonogram/save/design.md. Bump [VERSION] whenever the layout changes. */
object NonogramSaveCodec {
    private const val VERSION = 2
    private const val HEADER = "nonogram-save $VERSION"
    private const val HEADER_V1 = "nonogram-save 1"
    private const val NO_PICTURE = "-"

    fun encode(state: NonogramState, elapsedMillis: Long): String = listOf(
        HEADER,
        state.puzzle.size.toString(),
        state.pictureId ?: NO_PICTURE,
        state.puzzle.solution.joinToString("") { if (it) "1" else "0" },
        state.cells.joinToString("") { it.ordinal.toString() },
        elapsedMillis.toString(),
    ).joinToString("\n", postfix = "\n")

    fun decode(text: String): Pair<NonogramState, Long>? {
        val lines = text.removeSuffix("\n").split("\n")
        return when (lines.firstOrNull()) {
            HEADER -> if (lines.size == 6) {
                decodeFields(lines[1], lines[2].takeUnless { it == NO_PICTURE }, lines[3], lines[4], lines[5])
            } else {
                null
            }
            HEADER_V1 -> if (lines.size == 5) {
                decodeFields(lines[1], null, lines[2], lines[3], lines[4])
            } else {
                null
            }
            else -> null
        }
    }

    private fun decodeFields(
        sizeText: String,
        pictureId: String?,
        solution: String,
        cells: String,
        elapsedText: String,
    ): Pair<NonogramState, Long>? {
        val size = sizeText.toIntOrNull()?.takeIf { it in SAVED_NONOGRAM_SIZES } ?: return null
        val cellCount = size * size
        if (solution.length != cellCount || !solution.all { it in "01" }) return null
        if (cells.length != cellCount || !cells.all { it in "012" }) return null
        val elapsed = elapsedText.toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val puzzle = NonogramPuzzle(size, solution.map { it == '1' })
        return NonogramState(puzzle, pictureId, cells.map { Mark.entries[it - '0'] }) to elapsed
    }
}
