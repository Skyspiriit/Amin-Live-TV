package com.enigma.aminlivetv

// Where each kind of playback gets its subtitles. A later translation module plugs in here: it
// would add a `PlayerKind` it can serve and return its own `SubtitleMode`, without changes to
// LivePlayer or YouTubePlayer's playback code.
internal enum class PlayerKind {
    // Media3 streams. PlayerView draws subtitle tracks carried in the stream (WebVTT, CEA-608/708).
    NATIVE,
    // YouTube's embedded player. It can show a video's own captions and translate them to Persian.
    YOUTUBE,
    // Played in another app or the browser. The provider's player chooses subtitles.
    EXTERNAL
}

internal enum class SubtitleMode {
    // The source's default: whatever the stream or player shows by itself.
    SOURCE_DEFAULT,
    // Persian subtitles from the video's own track or YouTube's translation of its captions.
    PERSIAN_WHEN_AVAILABLE,
    OFF
}

internal object SubtitlePolicy {
    // Live news keeps the source's default: YouTube doesn't translate live captions. Documentary,
    // film, series and topic tiles ask for Persian subtitles.
    fun forChannel(channel: Channel, kind: PlayerKind): SubtitleMode = when {
        kind != PlayerKind.YOUTUBE -> SubtitleMode.SOURCE_DEFAULT
        channel.categories.any { !it.main } ||
            (Category.SPORTS in channel.categories && channel.youtube != null && channel.youtube.channel == null) -> SubtitleMode.PERSIAN_WHEN_AVAILABLE
        else -> SubtitleMode.SOURCE_DEFAULT
    }

    // Iranian episodes start with captions off (the speech is Persian); the viewer can show the
    // video's own captions.
    fun forEpisode(viewerWantsCaptions: Boolean): SubtitleMode =
        if (viewerWantsCaptions) SubtitleMode.SOURCE_DEFAULT else SubtitleMode.OFF
}
