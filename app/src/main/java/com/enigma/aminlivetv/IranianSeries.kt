package com.enigma.aminlivetv

import android.content.Context
import org.json.JSONObject

internal data class IranianSeries(val id: String, val title: String, val episodes: List<Episode>) {
    val multipleSeasons = episodes.last().season > 1
    fun after(episode: Episode): Episode? =
        episodes.indexOf(episode).takeIf { it >= 0 }?.let { episodes.getOrNull(it + 1) }
}

internal data class Episode(val id: String, val season: Int, val number: Int, val videoId: String) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$videoId"
}

private val seriesIdPattern = Regex("[a-z0-9][a-z0-9-]{0,63}")
private val episodeVideoPattern = Regex("[A-Za-z0-9_-]{11}")

internal fun parseIranianSeries(document: JSONObject): List<IranianSeries> {
    if (!document.has("iranianSeries")) return emptyList()
    val entries = document.getJSONArray("iranianSeries")
    require(entries.length() <= 100)
    val seriesIds = HashSet<String>()
    val videos = HashSet<String>()
    return (0 until entries.length()).map { index ->
        val item = entries.getJSONObject(index)
        val id = item.get("id") as String
        val title = item.get("title") as String
        require(seriesIdPattern.matches(id) && seriesIds.add(id))
        require(title.isNotBlank() && title.length <= 100)
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
        require(episodes.first().season == 1 && episodes.first().number == 1)
        require(episodes.zipWithNext().all { (from, to) ->
            (to.season == from.season && to.number == from.number + 1) ||
                (to.season == from.season + 1 && to.number == 1)
        })
        IranianSeries(id, title, episodes)
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
