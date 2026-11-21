package com.enigma.aminlivetv

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitlePolicyTest {
    private fun channel(category: Category) =
        Channel("test", "آزمایش", setOf(category), null, emptyList(), Origin.SATELLITE, "unverified", 0L)

    @Test fun onlyNonLiveYouTubeTilesAskForPersianSubtitles() {
        assertEquals(SubtitleMode.SOURCE_DEFAULT, SubtitlePolicy.forChannel(channel(Category.NEWS), PlayerKind.YOUTUBE))
        for (group in listOf(Category.DOCUMENTARY, Category.MOVIES, Category.SERIES, Category.GAMES)) {
            assertEquals(SubtitleMode.PERSIAN_WHEN_AVAILABLE, SubtitlePolicy.forChannel(channel(group), PlayerKind.YOUTUBE))
        }
        assertEquals(SubtitleMode.SOURCE_DEFAULT, SubtitlePolicy.forChannel(channel(Category.DOCUMENTARY), PlayerKind.NATIVE))
        assertEquals(SubtitleMode.SOURCE_DEFAULT, SubtitlePolicy.forChannel(channel(Category.MOVIES), PlayerKind.EXTERNAL))
    }

    @Test fun iranianEpisodesKeepTheirCaptionsAndTurkishOnesAskForPersian() {
        assertEquals(SubtitleMode.SOURCE_DEFAULT, SubtitlePolicy.forEpisode(turkish = false, viewerWantsCaptions = true))
        assertEquals(SubtitleMode.OFF, SubtitlePolicy.forEpisode(turkish = false, viewerWantsCaptions = false))
        assertEquals(SubtitleMode.PERSIAN_WHEN_AVAILABLE, SubtitlePolicy.forEpisode(turkish = true, viewerWantsCaptions = true))
        assertEquals(SubtitleMode.OFF, SubtitlePolicy.forEpisode(turkish = true, viewerWantsCaptions = false))
    }
}
