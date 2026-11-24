package com.enigma.aminlivetv

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SportsCatalogueTest {
    private fun document() = JSONObject(File("../channels.json").readText())

    @Test fun sportsStaysOneFlatGridWithNativeChannelsFirst() {
        val catalogue = CatalogueParser.parse(document())
        val sports = catalogue.inCategory(Category.SPORTS)
        val firstYouTube = sports.indexOfFirst { it.youtube != null }
        assertTrue(firstYouTube > 0)
        assertTrue(sports.drop(firstYouTube).none { it.streams.isNotEmpty() })
        assertTrue(sports.count { it.youtube != null } >= 13)
        assertEquals(4, Category.entries.count { it.main })
        assertTrue(catalogue.inCategory(Category.CHESS).isEmpty())
        val chess = sports.single { it.id == "persian-chess" }
        assertEquals("UU4wXiMrci9gCRLOuLGqHa2Q", chess.youtube!!.playlist)
    }

    @Test fun legacyClientsKeepEveryExistingIdAndTheOriginalChessSource() {
        val json = document()
        val chess = (0 until json.getJSONArray("programmes2").length())
            .map { json.getJSONArray("programmes2").getJSONObject(it) }.single { it.getString("id") == "persian-chess" }
        assertEquals("chess", chess.getString("group"))
        assertEquals("sports", chess.getString("displayGroup"))
        assertEquals("UU4wXiMrci9gCRLOuLGqHa2Q", chess.getString("youtubePlaylist"))
        assertEquals(38, CatalogueParser.parse(json).iranianSeries.size)
    }

    @Test fun curatedSportsRejectAmbiguousOrMalformedVideoLists() {
        fun entry() = JSONObject().put("id", "sports-test").put("name", "آزمایش ورزش").put("group", "sports")
            .put("sourceAttribution", JSONArray().put("https://www.fifa.com/"))
            .put("youtubeVideos", JSONArray().put("T9BfcJiy1L4"))
        fun parse(e: JSONObject): Catalogue = CatalogueParser.parse(document().put("programmes2", JSONArray().put(e)))
        assertEquals(listOf("T9BfcJiy1L4"), parse(entry()).channels.single { it.id == "sports-test" }.youtube!!.videos)
        val invalid = listOf(
            entry().put("youtubeVideos", JSONArray()),
            entry().put("youtubeVideos", JSONArray().put("T9BfcJiy1L4").put("T9BfcJiy1L4")),
            entry().put("youtubeVideos", JSONArray().put("https://youtube.com/watch?v=T9BfcJiy1L4")),
            entry().put("youtubePlaylist", "PLabcdefghijklmnop"),
            entry().put("youtubeChannel", "UCpcTrCXblq78GZrTUTLWeBw")
        )
        for (value in invalid) assertThrows(IllegalArgumentException::class.java) { parse(value) }
    }

    @Test fun existingPersianSubtitleBehaviourAppliesToSportsReplaysAndMovedChess() {
        val sports = CatalogueParser.parse(document()).inCategory(Category.SPORTS)
        for (channel in sports.filter { it.youtube != null }) {
            assertEquals(SubtitleMode.PERSIAN_WHEN_AVAILABLE, SubtitlePolicy.forChannel(channel, PlayerKind.YOUTUBE))
        }
        val live = sports.first().copy(youtube = YouTube("UCpcTrCXblq78GZrTUTLWeBw", null))
        assertEquals(SubtitleMode.SOURCE_DEFAULT, SubtitlePolicy.forChannel(live, PlayerKind.YOUTUBE))
        assertEquals(SubtitleMode.SOURCE_DEFAULT, SubtitlePolicy.forChannel(live, PlayerKind.NATIVE))
        assertEquals(SubtitleMode.OFF, SubtitlePolicy.forEpisode(false))
    }
}
