package com.enigma.aminlivetv

// Presentation only: unavailable numbers have no playable Episode and never get a progress key.
internal data class EpisodeCard(val season: Int, val number: Int?, val episode: Episode?) {
    val key: String get() = "$season/${number ?: "season"}"
}

internal fun episodeCards(series: IranianSeries): List<EpisodeCard> {
    val seasons = series.episodes.groupBy { it.season }
    val lastSeason = maxOf(seasons.keys.max(), series.episodeCounts.keys.maxOrNull() ?: 1)
    return (1..lastSeason).flatMap { season ->
        val episodes = seasons[season].orEmpty().associateBy { it.number }
        val end = series.episodeCounts[season] ?: episodes.keys.maxOrNull()
        if (end == null) listOf(EpisodeCard(season, null, null))
        else (1..end).map { number -> EpisodeCard(season, number, episodes[number]) }
    }
}

internal fun episodeTime(seconds: Double): String {
    val value = seconds.coerceAtLeast(0.0).toLong()
    val minutes = value / 60
    val parts = if (minutes >= 60) listOf(minutes / 60, minutes % 60, value % 60)
        else listOf(minutes, value % 60)
    return parts.mapIndexed { index, part ->
        val text = part.toString().let { if (index == 0) it else it.padStart(2, '0') }
        text.map { '۰' + (it - '0') }.joinToString("")
    }.joinToString(":")
}
