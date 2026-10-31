package dev.puzzlecollection.nonogram

import kotlin.math.floor
import kotlin.math.min

/** Board geometry in pixels. The clues sit left of and above the grid. */
class NonogramLayout(
    private val size: Int,
    maxRowClues: Int,
    maxColumnClues: Int,
    width: Float,
    height: Float,
) {
    val cell: Float = min(
        width / (size + maxRowClues * CLUE_RATIO),
        height / (size + maxColumnClues * CLUE_RATIO),
    )
    val clueStep: Float = cell * CLUE_RATIO
    val gridLeft: Float = clueStep * maxRowClues
    val gridTop: Float = clueStep * maxColumnClues
    val gridSize: Float = cell * size

    /** Null outside the grid, including over the clues. */
    fun cellAt(x: Float, y: Float): Int? {
        val column = floor((x - gridLeft) / cell).toInt()
        val row = floor((y - gridTop) / cell).toInt()
        return if (row in 0 until size && column in 0 until size) row * size + column else null
    }

    fun cellCenterX(cell: Int): Float = gridLeft + (cell % size + 0.5f) * this.cell
    fun cellCenterY(cell: Int): Float = gridTop + (cell / size + 0.5f) * this.cell

    private companion object {
        const val CLUE_RATIO = 0.62f
    }
}
