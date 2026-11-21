package com.enigma.aminlivetv

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesLibraryTest {
    @Test fun theBundledCatalogueKeepsEveryIranianSeries() {
        val series = parseIranianSeries(JSONObject(File("../channels.json").readText()))
        assertEquals(38, series.size)
        assertEquals(series.size, series.map { it.id }.distinct().size)
    }
}
