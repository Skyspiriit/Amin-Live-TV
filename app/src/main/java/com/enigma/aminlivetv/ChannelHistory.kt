package com.enigma.aminlivetv

import android.content.Context
import org.json.JSONObject
import kotlin.math.pow

internal enum class Availability { AVAILABLE, CHECKING, UNAVAILABLE }
internal enum class ProbeResult { PLAYABLE, FAILED, UNCERTAIN }

internal data class ViewingHistory(
    val sessions: Int = 0,
    val totalMs: Long = 0,
    val lastWatchedAt: Long = 0,
    val recentWeight: Double = 0.0
) {
    fun weightAt(now: Long): Double =
        recentWeight * 0.5.pow((now - lastWatchedAt).coerceAtLeast(0) / WEEK_MS.toDouble())

    fun add(now: Long, playedMs: Long, newSession: Boolean): ViewingHistory =
        ViewingHistory(
            sessions + if (newSession) 1 else 0,
            totalMs + playedMs,
            now,
            weightAt(now) + playedMs / 60_000.0 + if (newSession) 2.0 else 0.0
        )
}

internal data class HealthRecord(
    val sourceSet: String? = null,
    val availability: Availability = Availability.CHECKING,
    val checkedAt: Long = 0,
    val nextCheckAt: Long = 0,
    val failedChecks: Int = 0,
    val lastSource: String? = null,
    val playbackFailures: Int = 0,
    val lastPlaybackFailureAt: Long = 0,
    val viewing: ViewingHistory = ViewingHistory()
) {
    fun trackedFor(channel: Channel, now: Long): HealthRecord {
        if (sourceSet == channel.sourceSet) return this
        val startupDelay = if (channel.origin == Origin.DOMESTIC && channel.verifiedAt in 1..now) {
            channel.verifiedAt + 48 * HOUR_MS
        } else 0L
        return HealthRecord(
            sourceSet = channel.sourceSet,
            nextCheckAt = startupDelay,
            lastSource = lastSource?.takeIf { key -> channel.streams.any { it.key == key } },
            viewing = viewing
        )
    }

    fun canShow(channel: Channel): Boolean =
        channel.streams.isNotEmpty() && availability != Availability.UNAVAILABLE &&
            (channel.status == "verified" || availability == Availability.AVAILABLE || lastSource != null)

    fun favored(now: Long): Boolean =
        viewing.lastWatchedAt > 0 &&
            (now - viewing.lastWatchedAt <= 48 * HOUR_MS || viewing.weightAt(now) >= 3.0)

    fun probeFinished(channel: Channel, result: ProbeResult, now: Long, timings: ScanTimings): HealthRecord {
        val favorite = favored(now)
        val streak = if (result == ProbeResult.FAILED) (failedChecks + 1).coerceAtMost(6) else failedChecks
        val delay = when (result) {
            ProbeResult.PLAYABLE -> when {
                channel.origin == Origin.DOMESTIC -> 48 * HOUR_MS
                favorite -> 6 * HOUR_MS
                else -> 12 * HOUR_MS
            }
            ProbeResult.FAILED -> {
                val base = if (favorite) 12 * MINUTE_MS else 40 * MINUTE_MS
                (base * (1L shl (streak - 1))).coerceAtMost(12 * HOUR_MS)
            }
            ProbeResult.UNCERTAIN -> if (availability == Availability.UNAVAILABLE) {
                if (favorite) 12 * MINUTE_MS else 40 * MINUTE_MS
            } else if (favorite) 8 * MINUTE_MS else 25 * MINUTE_MS
        }
        return copy(
            availability = when (result) {
                ProbeResult.PLAYABLE -> Availability.AVAILABLE
                ProbeResult.FAILED -> Availability.UNAVAILABLE
                ProbeResult.UNCERTAIN -> availability
            },
            checkedAt = now,
            nextCheckAt = now + timings.interval(delay),
            failedChecks = if (result == ProbeResult.PLAYABLE) 0 else streak
        )
    }

    fun playbackExhausted(channel: Channel, confirmed: Boolean, now: Long, timings: ScanTimings): HealthRecord =
        if (confirmed) {
            probeFinished(channel, ProbeResult.FAILED, now, timings)
        } else {
            copy(
                nextCheckAt = now + timings.interval(if (favored(now)) MINUTE_MS else 5 * MINUTE_MS)
            )
        }

    fun order(streams: List<Stream>): List<Stream> = streams.sortedBy { it.key != lastSource }
}

internal data class ScanCandidate(val channel: Channel, val history: HealthRecord)

internal object ScanPlanner {
    fun pick(candidates: List<ScanCandidate>, now: Long, useFairSlot: Boolean): ScanCandidate? {
        val due = candidates.filter { it.history.nextCheckAt <= now }
        if (useFairSlot) {
            return due.minWithOrNull(compareBy({ it.history.checkedAt }, { it.history.nextCheckAt }, { it.channel.id }))
        }
        return due.minWithOrNull(compareByDescending<ScanCandidate> {
            val history = it.history
            val needsRecovery = history.availability == Availability.UNAVAILABLE ||
                (history.lastPlaybackFailureAt > history.checkedAt && history.lastPlaybackFailureAt > 0)
            when {
                history.favored(now) && needsRecovery -> 4
                history.favored(now) -> 3
                needsRecovery -> 2
                it.channel.origin == Origin.SATELLITE -> 1
                else -> 0
            }
        }.thenByDescending { it.history.viewing.weightAt(now) }
            .thenBy { it.history.checkedAt }
            .thenBy { it.channel.id })
    }
}

internal class ScanTimings(private val fixture: Boolean) {
    val initialDelayMs = if (fixture) 2200L else 4000L
    val gapMs = if (fixture) 1000L else 15_000L
    val windowMs = if (fixture) 15_000L else 10 * MINUTE_MS
    val limit = 6

    fun interval(productionMs: Long): Long =
        if (fixture) (productionMs / 180).coerceAtLeast(2200L) else productionMs
}

internal class ChannelHistory(context: Context, private val timings: ScanTimings) {
    private val prefs = context.getSharedPreferences("channel-health", Context.MODE_PRIVATE)
    private val records = HashMap<String, HealthRecord>()

    fun forChannel(channel: Channel, now: Long): HealthRecord =
        records.getOrPut(channel.id) { load(channel.id) }.trackedFor(channel, now)

    fun isVisible(channel: Channel, now: Long): Boolean =
        forChannel(channel, now).canShow(channel)

    fun probeFinished(channel: Channel, result: ProbeResult, now: Long) {
        save(channel.id, forChannel(channel, now).probeFinished(channel, result, now, timings))
    }

    fun played(channel: Channel, source: Stream, now: Long) {
        val current = forChannel(channel, now).probeFinished(channel, ProbeResult.PLAYABLE, now, timings)
        save(channel.id, current.copy(lastSource = source.key))
    }

    fun sourceFailed(channel: Channel, now: Long) {
        val current = forChannel(channel, now)
        val recent = if (now - current.lastPlaybackFailureAt <= WEEK_MS) current.playbackFailures else 0
        save(channel.id, current.copy(playbackFailures = (recent + 1).coerceAtMost(999), lastPlaybackFailureAt = now))
    }

    fun exhausted(channel: Channel, confirmed: Boolean, now: Long) {
        save(channel.id, forChannel(channel, now).playbackExhausted(channel, confirmed, now, timings))
    }

    fun viewed(channel: Channel, playedMs: Long, newSession: Boolean, now: Long) {
        val current = forChannel(channel, now)
        save(channel.id, current.copy(viewing = current.viewing.add(now, playedMs, newSession)))
    }

    private fun load(id: String): HealthRecord = runCatching {
        val json = JSONObject(prefs.getString(id, null) ?: return HealthRecord())
        HealthRecord(
            sourceSet = json.optString("sources", ""),
            availability = Availability.valueOf(json.optString("state", Availability.CHECKING.name)),
            checkedAt = json.optLong("checked"),
            nextCheckAt = json.optLong("next"),
            failedChecks = json.optInt("failed"),
            lastSource = json.optString("source", "").takeIf { it.isNotEmpty() },
            playbackFailures = json.optInt("playbackFailures"),
            lastPlaybackFailureAt = json.optLong("playbackFailureAt"),
            viewing = ViewingHistory(
                json.optInt("sessions"), json.optLong("total"), json.optLong("watched"), json.optDouble("weight", 0.0)
            )
        )
    }.getOrDefault(HealthRecord())

    private fun save(id: String, item: HealthRecord) {
        records[id] = item
        val json = JSONObject()
            .put("sources", item.sourceSet.orEmpty())
            .put("state", item.availability.name)
            .put("checked", item.checkedAt)
            .put("next", item.nextCheckAt)
            .put("failed", item.failedChecks)
            .put("source", item.lastSource.orEmpty())
            .put("playbackFailures", item.playbackFailures)
            .put("playbackFailureAt", item.lastPlaybackFailureAt)
            .put("sessions", item.viewing.sessions)
            .put("total", item.viewing.totalMs)
            .put("watched", item.viewing.lastWatchedAt)
            .put("weight", item.viewing.recentWeight)
        prefs.edit().putString(id, json.toString()).apply()
    }
}

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val WEEK_MS = 7 * 24 * HOUR_MS
