package com.enigma.aminlivetv

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesLibraryTest {
    private fun episodes(vararg videos: String) = JSONArray().apply {
        videos.forEachIndexed { index, video ->
            put(JSONObject().put("id", "s1e${index + 1}").put("season", 1).put("number", index + 1).put("videoId", video))
        }
    }

    private fun series(id: String, vararg videos: String) =
        JSONObject().put("id", id).put("title", "عنوان").put("episodes", episodes(*videos))

    @Test fun turkishSeriesAreAParallelLibraryWithTheSameRules() {
        val document = JSONObject()
            .put("iranianSeries", JSONArray().put(series("hezartou", "D_VP8L4z0X0")))
            .put("turkishSeries", JSONArray().put(series("kara-para-ask", "BEFCZSkcJWc", "aaaaaaaaaaa")))
            .put("turkishSeriesInProgress", JSONArray().put(series("hercai", "bbbbbbbbbbb").put("status", "partial")))
        val (iranian, turkish) = parseSeriesLibraries(document)
        assertEquals(listOf("hezartou"), iranian.map { it.id })
        assertEquals(listOf("kara-para-ask", "hercai"), turkish.map { it.id })
        assertEquals(SeriesAvailability.PARTIAL, turkish[1].availability)
        assertEquals(iranian, parseIranianSeries(document))
    }

    @Test fun aSeriesIdOrVideoCannotBeInBothLibraries() {
        val sameId = JSONObject()
            .put("iranianSeries", JSONArray().put(series("shared", "D_VP8L4z0X0")))
            .put("turkishSeries", JSONArray().put(series("shared", "BEFCZSkcJWc")))
        assertTrue(runCatching { parseSeriesLibraries(sameId) }.isFailure)
        val sameVideo = JSONObject()
            .put("iranianSeries", JSONArray().put(series("one", "D_VP8L4z0X0")))
            .put("turkishSeries", JSONArray().put(series("two", "D_VP8L4z0X0")))
        assertTrue(runCatching { parseSeriesLibraries(sameVideo) }.isFailure)
    }

    @Test fun aCompleteTurkishSeriesMustBeConsecutive() {
        val gap = series("gap", "D_VP8L4z0X0", "BEFCZSkcJWc")
        gap.getJSONArray("episodes").getJSONObject(1).put("number", 3)
        assertTrue(runCatching { parseSeriesLibraries(JSONObject().put("turkishSeries", JSONArray().put(gap))) }.isFailure)
    }

    @Test fun theBundledCatalogueKeepsEveryIranianSeries() {
        val document = JSONObject(File("../channels.json").readText())
        val (iranian, turkish) = parseSeriesLibraries(document)
        assertEquals(38, iranian.size)
        assertTrue(turkish.isEmpty())
    }
}
