package com.enigma.aminlivetv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianSeriesTest {
    private val finale = Episode("s1e1", 1, 1, "D_VP8L4z0X0")
    private val following = Episode("s2e1", 2, 1, "BEFCZSkcJWc")
    private val series = IranianSeries("test-series", "مجموعه", listOf(finale, following))

    @Test fun nextEpisodeCrossesSeasonBoundaryAndStopsAtSeriesEnd() {
        assertTrue(series.multipleSeasons)
        assertEquals(following, series.after(finale))
        assertNull(series.after(following))
        assertNull(series.after(finale.copy(id = "removed")))
        assertEquals("۱۳", persianNumber(13))
    }

    @Test fun aBookmarkCanOnlyResumeItsExactSeriesEpisodeAndVideo() {
        val saved = EpisodePosition(series.id, finale.id, finale.videoId, 217.5, false, 15)
        assertTrue(saved.matches(series, finale))
        assertFalse(saved.matches(series, following))
        assertFalse(saved.matches(series, finale.copy(videoId = following.videoId)))
        assertFalse(saved.matches(series.copy(id = "other-series"), finale))
    }
}
