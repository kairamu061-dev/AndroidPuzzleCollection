package dev.puzzlecollection.sudoku

data class SavedGame(val state: GameState, val elapsedMillis: Long)

/** Selection, memo mode, and undo history are deliberately not part of a save. */
fun shouldSave(before: GameState, after: GameState): Boolean = before.board != after.board

/**
 * Format: docs/sudoku/save/design.md. Changing the layout requires bumping [VERSION]:
 * [decode] rejects any other version, and a silently reinterpreted save would corrupt a game.
 */
object SaveCodec {
    private const val VERSION = 1
    private const val HEADER = "sudoku-save $VERSION"
    private const val LINE_COUNT = 7
    private val ALL_MEMOS = DIGITS.fold(0) { mask, digit -> mask or digitBit(digit) }

    fun encode(game: SavedGame): String {
        val state = game.state
        return listOf(
            HEADER,
            state.puzzle.difficulty.name,
            state.puzzle.givens.joinToString(""),
            state.puzzle.solution.joinToString(""),
            state.board.values.joinToString(""),
            state.board.memos.joinToString(","),
            game.elapsedMillis.toString(),
        ).joinToString("\n", postfix = "\n")
    }

    fun decode(text: String): SavedGame? {
        val lines = text.removeSuffix("\n").split("\n")
        if (lines.size != LINE_COUNT || lines[0] != HEADER) return null
        val difficulty = Difficulty.entries.firstOrNull { it.name == lines[1] } ?: return null
        val givens = decodeDigits(lines[2]) ?: return null
        val solution = decodeDigits(lines[3]) ?: return null
        val values = decodeDigits(lines[4]) ?: return null
        val memos = decodeMemos(lines[5]) ?: return null
        val elapsed = lines[6].toLongOrNull()?.takeIf { it >= 0 } ?: return null

        if (!isCompleteGrid(solution)) return null
        for (cell in 0 until CELL_COUNT) {
            if (givens[cell] != BLANK && (givens[cell] != solution[cell] || values[cell] != givens[cell])) {
                return null
            }
        }
        val puzzle = Puzzle(givens, solution, difficulty)
        return SavedGame(GameState(puzzle, BoardSnapshot(values, memos)), elapsed)
    }

    private fun decodeDigits(line: String): List<Int>? {
        if (line.length != CELL_COUNT || !line.all { it in '0'..'9' }) return null
        return line.map { it - '0' }
    }

    private fun decodeMemos(line: String): List<Int>? {
        val parts = line.split(",")
        if (parts.size != CELL_COUNT) return null
        return parts.map { part ->
            part.toIntOrNull()?.takeIf { it and ALL_MEMOS.inv() == 0 } ?: return null
        }
    }

    private fun isCompleteGrid(cells: List<Int>): Boolean =
        Geometry.units.all { unit -> unit.map { cells[it] }.toSet() == DIGITS.toSet() }
}
