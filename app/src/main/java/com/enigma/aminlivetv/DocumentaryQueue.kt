package com.enigma.aminlivetv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

internal enum class DocumentaryResult { WATCHED, SKIPPED, UNAVAILABLE }

internal data class DocumentaryRound(
    val videos: List<String> = emptyList(),
    val position: Int = 0
) {
    val current get() = videos.getOrNull(position)

    fun advance(): DocumentaryRound = if (current == null) this else copy(position = position + 1)
}

private const val ROUND_SIZE = 25
private const val MAX_CANDIDATES = 500
private val videoId = Regex("[A-Za-z0-9_-]{11}")

internal fun documentaryCandidates(videos: Sequence<String>): List<String> =
    videos.filter(videoId::matches).distinct().take(MAX_CANDIDATES).toList()

internal fun newDocumentaryRound(
    candidates: List<String>,
    unavailable: Set<String> = emptySet(),
    random: Random = Random.Default,
    lastViewed: (String) -> Long
): DocumentaryRound {
    val (unseen, seen) = documentaryCandidates(candidates.asSequence())
        .filterNot(unavailable::contains).map { it to lastViewed(it) }.partition { it.second == 0L }
    val first = unseen.shuffled(random).take(ROUND_SIZE).map { it.first }
    val fill = seen.shuffled(random).sortedBy { abs(it.second) }
        .take(ROUND_SIZE - first.size).map { it.first }.shuffled(random)
    return DocumentaryRound(first + fill)
}

internal class DocumentaryQueue(context: Context, tile: String, private val playlist: String) {
    private val preferences = context.getSharedPreferences("documentary-rounds", Context.MODE_PRIVATE)
    private val roundKey = "tile:$tile"
    private val saved = preferences.getString(roundKey, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        ?.takeIf { it.optString("playlist") == playlist }
    private var candidates = saved?.optJSONArray("candidates")?.videoIds().orEmpty()
    private var round = saved?.let {
        val videos = it.optJSONArray("videos")?.videoIds().orEmpty().take(ROUND_SIZE)
        DocumentaryRound(videos, it.optInt("position").coerceIn(0, videos.size))
    } ?: DocumentaryRound()

    val current get() = round.current

    fun renew(fresh: List<String>, unavailable: Set<String>) {
        if (fresh.isNotEmpty()) candidates = fresh
        round = newDocumentaryRound(candidates, unavailable) { preferences.getLong("video:$it", 0L) }
        preferences.edit().putString(roundKey, encode()).apply()
    }

    fun advance(result: DocumentaryResult) {
        val video = current ?: return
        round = round.advance()
        val edit = preferences.edit().putString(roundKey, encode())
        if (result != DocumentaryResult.UNAVAILABLE) {
            // Negative means skipped. Its magnitude and the positive watched value both sort by recency.
            val order = preferences.getLong("order", 0L) + 1L
            edit.putLong("order", order)
                .putLong("video:$video", if (result == DocumentaryResult.SKIPPED) -order else order)
        }
        edit.apply()
    }

    private fun encode() = JSONObject().put("playlist", playlist)
        .put("candidates", JSONArray(candidates)).put("videos", JSONArray(round.videos))
        .put("position", round.position).toString()
}

private fun JSONArray.videoIds(): List<String> =
    documentaryCandidates((0 until length()).asSequence().map { optString(it) })
