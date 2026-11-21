package com.enigma.aminlivetv

import android.content.Context
import org.json.JSONObject

internal enum class SeriesAvailability { COMPLETE, PARTIAL, ONGOING }

internal data class IranianSeries(
    val id: String,
    val title: String,
    val episodes: List<Episode>,
    val availability: SeriesAvailability = SeriesAvailability.COMPLETE
) {
    val multipleSeasons = episodes.last().season > 1
    val thumbnail = "https://i.ytimg.com/vi/${episodes.first().videoId}/mqdefault.jpg"
    fun after(episode: Episode): Episode? =
        episodes.indexOf(episode).takeIf { it >= 0 }?.let { episodes.getOrNull(it + 1) }
}

internal data class Episode(val id: String, val season: Int, val number: Int, val videoId: String) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$videoId"
}

// Intent extras for IranianSeriesActivity.
internal const val EXTRA_LIBRARY = "library"
internal const val LIBRARY_TURKISH = "turkish"
internal const val EXTRA_RESUME = "resume"

private val seriesIdPattern = Regex("[a-z0-9][a-z0-9-]{0,63}")
private val episodeVideoPattern = Regex("[A-Za-z0-9_-]{11}")

internal fun parseIranianSeries(document: JSONObject): List<IranianSeries> =
    parseSeriesLibraries(document).first

// Iranian and Turkish series use the same episode format and progress store,
// so a series ID may appear in only one of the two libraries. Releases before 3.9 ignore the
// Turkish keys.
internal fun parseSeriesLibraries(document: JSONObject): Pair<List<IranianSeries>, List<IranianSeries>> {
    val videos = HashSet<String>()
    val iranianIds = HashSet<String>()
    // Older releases understand only complete, consecutive episode lists.
    val iranian = parseSeriesSection(document, "iranianSeries", complete = true, iranianIds, videos) +
        parseSeriesSection(document, "iranianSeriesInProgress", complete = false, iranianIds, videos)
    val turkishIds = HashSet<String>()
    val turkish = parseSeriesSection(document, "turkishSeries", complete = true, turkishIds, videos) +
        parseSeriesSection(document, "turkishSeriesInProgress", complete = false, turkishIds, videos)
    require(turkishIds.none { it in iranianIds })
    return iranian to turkish
}

private fun parseSeriesSection(
    document: JSONObject,
    key: String,
    complete: Boolean,
    seriesIds: MutableSet<String>,
    videos: MutableSet<String>
): List<IranianSeries> {
    if (!document.has(key)) return emptyList()
    val entries = document.getJSONArray(key)
    require(seriesIds.size + entries.length() <= 100)
    return (0 until entries.length()).map { index ->
        val item = entries.getJSONObject(index)
        val id = item.get("id") as String
        val title = item.get("title") as String
        require(seriesIdPattern.matches(id) && seriesIds.add(id))
        require(title.isNotBlank() && title.length <= 100)
        val availability = if (complete) SeriesAvailability.COMPLETE else when (item.getString("status")) {
            "partial" -> SeriesAvailability.PARTIAL
            "ongoing" -> SeriesAvailability.ONGOING
            else -> throw IllegalArgumentException("Unknown series status")
        }
        val items = item.getJSONArray("episodes")
        require(items.length() in 1..500)
        val episodeIds = HashSet<String>()
        val episodes = (0 until items.length()).map { position ->
            val entry = items.getJSONObject(position)
            val episodeId = entry.get("id") as String
            val season = entry.get("season") as Int
            val number = entry.get("number") as Int
            val videoId = entry.get("videoId") as String
            require(seriesIdPattern.matches(episodeId) && episodeIds.add(episodeId))
            require(season in 1..50 && number in 1..500)
            require(episodeVideoPattern.matches(videoId) && videos.add(videoId))
            Episode(episodeId, season, number, videoId)
        }.sortedWith(compareBy(Episode::season, Episode::number))
        require(episodes.distinctBy { it.season to it.number }.size == episodes.size)
        if (complete) {
            require(episodes.first().season == 1 && episodes.first().number == 1)
            require(episodes.zipWithNext().all { (from, to) ->
                (to.season == from.season && to.number == from.number + 1) ||
                    (to.season == from.season + 1 && to.number == 1)
            })
        }
        IranianSeries(id, title, episodes, availability)
    }
}

internal data class EpisodePosition(
    val seriesId: String,
    val episodeId: String,
    val videoId: String,
    val seconds: Double,
    val completed: Boolean,
    val updated: Long
) {
    fun matches(series: IranianSeries, episode: Episode): Boolean =
        seriesId == series.id && episodeId == episode.id && videoId == episode.videoId
}

internal class SeriesProgress(context: Context) {
    private val preferences = context.getSharedPreferences("iranian-series-progress", Context.MODE_PRIVATE)

    fun position(series: IranianSeries, episode: Episode): EpisodePosition? =
        decode(preferences.getString(key(series, episode), null))?.takeIf { it.matches(series, episode) }

    fun continuation(series: IranianSeries): Pair<Episode, EpisodePosition>? =
        series.episodes.mapNotNull { episode -> position(series, episode)?.let { episode to it } }
            .maxByOrNull { (_, position) -> position.updated }

    // The episode «ادامهٔ تماشا» opens: the saved one, or the next one once it was finished.
    fun resumable(series: IranianSeries): Pair<Episode, EpisodePosition>? {
        val (last, position) = continuation(series) ?: return null
        val target = if (position.completed) series.after(last) ?: return null else last
        return target to position
    }

    // The most recently watched series in a library that still has an episode to continue.
    fun latest(library: List<IranianSeries>): Pair<IranianSeries, Episode>? =
        library.mapNotNull { series -> resumable(series)?.let { (episode, position) -> Triple(series, episode, position) } }
            .maxByOrNull { it.third.updated }?.let { (series, episode) -> series to episode }

    fun save(series: IranianSeries, episode: Episode, seconds: Double, completed: Boolean) {
        val entry = JSONObject().put("seriesId", series.id).put("episodeId", episode.id)
            .put("videoId", episode.videoId).put("seconds", seconds).put("completed", completed)
            .put("updated", System.currentTimeMillis())
        preferences.edit().putString(key(series, episode), entry.toString()).apply()
    }

    fun clear(series: IranianSeries, episode: Episode) {
        preferences.edit().remove(key(series, episode)).apply()
    }

    private fun key(series: IranianSeries, episode: Episode) = "${series.id}/${episode.id}"

    private fun decode(value: String?): EpisodePosition? = value?.let {
        runCatching {
            val json = JSONObject(it)
            val seconds = json.getDouble("seconds")
            require(seconds.isFinite() && seconds >= 0)
            EpisodePosition(json.getString("seriesId"), json.getString("episodeId"), json.getString("videoId"),
                seconds, json.getBoolean("completed"), json.getLong("updated"))
        }.getOrNull()
    }
}

internal fun persianNumber(number: Int): String =
    number.toString().map { '۰' + (it - '0') }.joinToString("")
