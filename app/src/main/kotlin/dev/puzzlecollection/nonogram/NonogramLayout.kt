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
    fun cellAt(x: Float, y: Float, viewport: NonogramViewport = NonogramViewport()): Int? {
        if (x < gridLeft || y < gridTop) return null
        if (x >= gridLeft + gridSize || y >= gridTop + gridSize) return null
        val zoomed = cell * viewport.scale
        val column = floor((x - gridLeft + viewport.panX) / zoomed).toInt()
        val row = floor((y - gridTop + viewport.panY) / zoomed).toInt()
        return if (row in 0 until size && column in 0 until size) row * size + column else null
    }

    fun cellCenterX(cell: Int): Float = gridLeft + (cell % size + 0.5f) * this.cell
    fun cellCenterY(cell: Int): Float = gridTop + (cell / size + 0.5f) * this.cell

    private companion object {
        const val CLUE_RATIO = 0.62f
    }
}

/** Which part of the grid shows. Clues stay put; only the grid area scrolls. Pan is in pixels. */
data class NonogramViewport(val scale: Float = 1f, val panX: Float = 0f, val panY: Float = 0f) {
    val isZoomed: Boolean get() = scale > 1f

    /** Keeps the grid point under the fingers in place while the scale changes. */
    fun zoomedBy(factor: Float, focusX: Float, focusY: Float, layout: NonogramLayout): NonogramViewport {
        val nextScale = (scale * factor).coerceIn(1f, MAX_SCALE)
        val ratio = nextScale / scale
        val x = focusX - layout.gridLeft
        val y = focusY - layout.gridTop
        return copy(scale = nextScale, panX = (x + panX) * ratio - x, panY = (y + panY) * ratio - y)
            .clamped(layout)
    }

    fun pannedBy(dx: Float, dy: Float, layout: NonogramLayout): NonogramViewport =
        copy(panX = panX - dx, panY = panY - dy).clamped(layout)

    private fun clamped(layout: NonogramLayout): NonogramViewport {
        val limit = layout.gridSize * (scale - 1f)
        return copy(panX = panX.coerceIn(0f, limit), panY = panY.coerceIn(0f, limit))
    }

    companion object {
        const val MAX_SCALE = 4f
    }
}
