package com.enigma.aminlivetv

internal class ViewingSession(private val record: (Long, Boolean) -> Unit) {
    private var startedAt: Long? = null
    private var playedMs = 0L
    private var savedMs = 0L

    fun start(now: Long) {
        if (startedAt == null) startedAt = now
    }

    fun pause(now: Long) {
        startedAt?.let { playedMs += (now - it).coerceAtLeast(0) }
        startedAt = null
    }

    fun save(now: Long, finished: Boolean = false) {
        val elapsed = playedMs + (startedAt?.let { (now - it).coerceAtLeast(0) } ?: 0)
        if (elapsed < MIN_VIEWING_MS) return
        val unsaved = elapsed - savedMs
        if (unsaved <= 0 || (!finished && savedMs > 0 && unsaved < SAVE_INTERVAL_MS)) return
        record(unsaved, savedMs == 0L)
        savedMs = elapsed
    }

    private companion object {
        const val MIN_VIEWING_MS = 30_000L
        const val SAVE_INTERVAL_MS = 60_000L
    }
}
