package com.enigma.aminlivetv

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class EpisodeCardsTest {
    private fun episode(season: Int, number: Int) = Episode("s${season}e$number", season, number, "D_VP8L4z0X0")

    @Test fun gapsStayVisibleWithoutRenumberingOrChangingPlayableIds() {
        val first = episode(1, 1)
        val third = episode(1, 3)
        val series = IranianSeries("test", "آزمایش", listOf(third, first), SeriesAvailability.PARTIAL)
        val cards = episodeCards(series)
        assertEquals(listOf(1, 2, 3), cards.map { it.number })
        assertSame(first, cards[0].episode)
        assertNull(cards[1].episode)
        assertSame(third, cards[2].episode)
        assertEquals("s1e3", cards[2].episode!!.id)
    }

    @Test fun aLeadingMissingUploadDoesNotTurnEpisodeTwoIntoEpisodeOne() {
        val cards = episodeCards(IranianSeries("test", "آزمایش", listOf(episode(1, 2)), SeriesAvailability.PARTIAL))
        assertEquals(2, cards.size)
        assertNull(cards[0].episode)
        assertEquals(2, cards[1].episode!!.number)
    }

    @Test fun anEntireMissingSeasonDoesNotInventItsEpisodeCount() {
        val cards = episodeCards(IranianSeries("test", "آزمایش", listOf(episode(2, 1)), SeriesAvailability.PARTIAL))
        assertEquals(EpisodeCard(1, null, null), cards.first())
        assertEquals(1, cards.last().number)
        assertEquals(2, cards.last().season)
    }

    @Test fun verifiedBoundsCanExposeAnUnavailableFinaleWithoutChangingEpisodes() {
        val first = episode(1, 1)
        val cards = episodeCards(IranianSeries("test", "آزمایش", listOf(first), SeriesAvailability.PARTIAL, mapOf(1 to 3)))
        assertEquals(listOf(1, 2, 3), cards.map { it.number })
        assertEquals(1, cards.count { it.episode != null })
        assertEquals(cards.size, cards.map { it.key }.distinct().size)
    }

    @Test fun absentBoundsDoNotInventFutureEpisodesOfAnOngoingSeries() {
        val cards = episodeCards(IranianSeries("test", "آزمایش", listOf(episode(1, 3)), SeriesAvailability.ONGOING))
        assertEquals(3, cards.last().number)
        assertEquals(3, cards.size)
    }

    @Test fun optionalSeasonBoundsAreValidatedByTheRealCatalogueParser() {
        fun document(counts: String) = JSONObject("""{"iranianSeriesInProgress":[{"id":"test","title":"آزمایش","status":"partial","episodeCounts":$counts,"episodes":[{"id":"s1e2","season":1,"number":2,"videoId":"D_VP8L4z0X0"}]}]}""")
        val title = parseIranianSeries(document("""{"1":3}""")).single()
        assertEquals(3, episodeCards(title).size)
        for (invalid in listOf("""{"1":1}""", """{"01":3}""", """{"0":3}""", """{"1":501}""")) {
            assertThrows(IllegalArgumentException::class.java) { parseIranianSeries(document(invalid)) }
        }
    }

    @Test fun displayedTimeUsesPersianDigitsWithoutRoundingTheStoredBookmark() {
        assertEquals("۲:۱۷", episodeTime(137.9))
        assertEquals("۱:۰۲:۰۳", episodeTime(3723.5))
        assertEquals("۰:۰۰", episodeTime(0.0))
    }
}
