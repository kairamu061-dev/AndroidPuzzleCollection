package dev.puzzlecollection.numberlink

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Square grid centred in a [width] by [height] pixel area. */
class NumberlinkLayout(private val size: Int, width: Float, height: Float) {
    val cell: Float = min(width, height) / size
    val left: Float = (width - cell * size) / 2
    val top: Float = (height - cell * size) / 2

    fun cellAt(x: Float, y: Float): Int? {
        val column = floor((x - left) / cell).toInt()
        val row = floor((y - top) / cell).toInt()
        return if (row in 0 until size && column in 0 until size) row * size + column else null
    }

    fun centerX(cell: Int): Float = left + (cell % size + 0.5f) * this.cell
    fun centerY(cell: Int): Float = top + (cell / size + 0.5f) * this.cell

    /**
     * Cells crossed moving from one point to another, in order, excluding the start cell.
     * A fast swipe arrives as one long jump; walking it keeps the line unbroken.
     */
    fun cellsAlong(fromX: Float, fromY: Float, toX: Float, toY: Float): List<Int> {
        val steps = max(1, ceil(max(abs(toX - fromX), abs(toY - fromY)) / (cell / 4)).toInt())
        val start = cellAt(fromX, fromY)
        val cells = mutableListOf<Int>()
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val at = cellAt(fromX + (toX - fromX) * t, fromY + (toY - fromY) * t) ?: continue
            if (at != start && at != cells.lastOrNull()) cells += at
        }
        return cells
    }
}
