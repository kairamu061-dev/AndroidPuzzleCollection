package dev.puzzlecollection.common

/** `now` is any monotonic millisecond clock; wall-clock time would jump on a clock change. */
class PlayTimer(initialElapsedMillis: Long = 0) {
    private var accumulated = initialElapsedMillis
    private var startedAt: Long? = null

    fun start(now: Long) {
        if (startedAt == null) startedAt = now
    }

    fun pause(now: Long) {
        accumulated = elapsed(now)
        startedAt = null
    }

    fun elapsed(now: Long): Long = accumulated + (startedAt?.let { now - it } ?: 0)
}
