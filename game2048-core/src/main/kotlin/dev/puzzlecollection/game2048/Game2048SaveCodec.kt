package dev.puzzlecollection.game2048

/** Format: docs/game2048/save/design.md. Bump [VERSION] whenever the layout changes. */
object Game2048SaveCodec {
    private const val VERSION = 1
    private const val HEADER = "2048-save $VERSION"
    private const val LINE_COUNT = 5

    fun encode(state: Game2048State): String = listOf(
        HEADER,
        state.values.joinToString(","),
        state.score.toString(),
        state.best.toString(),
        if (state.reached2048) "1" else "0",
    ).joinToString("\n", postfix = "\n")

    fun decode(text: String): Game2048State? {
        val lines = text.removeSuffix("\n").split("\n")
        if (lines.size != LINE_COUNT || lines[0] != HEADER) return null
        val values = lines[1].split(",").map { it.toIntOrNull() ?: return null }
        if (values.size != BOARD_SIZE * BOARD_SIZE || !values.all(::isTileValue)) return null
        val score = lines[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val best = lines[3].toIntOrNull()?.takeIf { it >= score } ?: return null
        val reached = when (lines[4]) {
            "0" -> false
            "1" -> true
            else -> return null
        }
        val tiles = values.mapIndexedNotNull { cell, value ->
            if (value == 0) null else Tile(cell + 1, value, cell / BOARD_SIZE, cell % BOARD_SIZE)
        }
        return Game2048State(tiles, score, best, reached)
    }

    private fun isTileValue(value: Int): Boolean =
        value == 0 || (value >= 2 && Integer.bitCount(value) == 1)
}
