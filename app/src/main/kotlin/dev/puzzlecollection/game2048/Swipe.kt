package dev.puzzlecollection.game2048

import kotlin.math.abs

/** The drag's dominant axis decides the direction; shorter than [threshold] is not a swipe. */
fun dragDirection(dx: Float, dy: Float, threshold: Float): Direction? = when {
    maxOf(abs(dx), abs(dy)) < threshold -> null
    abs(dx) > abs(dy) -> if (dx > 0) Direction.RIGHT else Direction.LEFT
    else -> if (dy > 0) Direction.DOWN else Direction.UP
}
