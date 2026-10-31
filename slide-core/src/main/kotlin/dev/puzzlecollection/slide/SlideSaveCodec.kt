package dev.puzzlecollection.slide

/** Format: docs/slide/save/design.md. Bump [VERSION] whenever the layout changes. */
object SlideSaveCodec {
    private const val VERSION = 1
    private const val HEADER = "slide-save $VERSION"
    private const val LINE_COUNT = 5

    fun encode(state: SlideState, elapsedMillis: Long): String = listOf(
        HEADER,
        state.size.toString(),
        state.tiles.joinToString(","),
        state.moves.toString(),
        elapsedMillis.toString(),
    ).joinToString("\n", postfix = "\n")

    fun decode(text: String): Pair<SlideState, Long>? {
        val lines = text.removeSuffix("\n").split("\n")
        if (lines.size != LINE_COUNT || lines[0] != HEADER) return null
        val size = lines[1].toIntOrNull()?.takeIf { it in SLIDE_SIZES } ?: return null
        val tiles = lines[2].split(",").map { it.toIntOrNull() ?: return null }
        if (tiles.sorted() != (0 until size * size).toList()) return null
        val moves = lines[3].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val elapsed = lines[4].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        return SlideState(size, tiles, moves) to elapsed
    }
}
