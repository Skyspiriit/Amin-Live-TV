package com.enigma.aminlivetv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelPolicyTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L
    private val first = Stream("https://broadcaster.example/main", "hls", 1)
    private val backup = Stream("https://backup.example/live", "hls", 2)
    private val timings = ScanTimings(false)

    @Test fun internationalNewsCanBeListedWithoutChangingNativeStreamHealth() {
        val domestic = station("irinn").copy(origin = Origin.DOMESTIC)
        val bbc = station(Channel.BBC_PERSIAN).copy(
            streams = emptyList(), status = "restricted",
            officialLiveUrl = "https://www.bbc.com/persian/watch/bbc_persian_tv/live")
        val international = station("international").copy(categories = setOf(Category.NEWS))
        val multi = station("mixed").copy(categories = setOf(Category.FILM, Category.NEWS))
        val catalogue = Catalogue(4, listOf(domestic, bbc, international, multi))
        assertEquals(listOf(domestic, international, bbc, multi), catalogue.inCategory(Category.NEWS))
        assertEquals(listOf(multi), catalogue.inCategory(Category.FILM))
        assertFalse(HealthRecord().canShow(bbc))
        assertTrue(bbc.officialLiveUrl != null)
        assertTrue(international.listedInApp)
    }

    @Test fun filmOrderSurvivesCatalogueRefreshAndChannelRecovery() {
        val persiana = station("persiana-family").copy(categories = setOf(Category.FILM), filmOrder = 1010, status = "verified")
        val gemFirst = station("gem-tv").copy(categories = setOf(Category.FILM), filmOrder = 2010, status = "verified")
        val gemNext = station("gem-series").copy(categories = setOf(Category.FILM), filmOrder = 2020, streams = emptyList())
        val other = station("ifilm").copy(categories = setOf(Category.FILM), filmOrder = 3010, status = "verified")
        val expected = listOf(persiana, gemFirst, gemNext, other)
        val original = Catalogue(3, expected.reversed())
        val refreshed = Catalogue(4, listOf(gemNext, other, persiana, gemFirst))
        assertEquals(expected, original.inCategory(Category.FILM))
        assertEquals(expected, refreshed.inCategory(Category.FILM))

        val ordered = refreshed.inCategory(Category.FILM)
        val failed = HealthRecord().probeFinished(gemFirst, ProbeResult.FAILED, now, timings)
        val recovered = failed.probeFinished(gemFirst, ProbeResult.PLAYABLE, now + day, timings)
        assertEquals(listOf(persiana, other), ordered.filter {
            (if (it == gemFirst) failed else HealthRecord()).canShow(it)
        })
        assertEquals(listOf(persiana, gemFirst, other), ordered.filter {
            (if (it == gemFirst) recovered else HealthRecord()).canShow(it)
        })
    }

    @Test fun visibleChannelsNeedATrustedSourceAndSurviveInconclusiveChecks() {
        val channel = station("test")
        val initial = HealthRecord().trackedFor(channel, now)
        assertFalse(initial.canShow(channel))
        assertTrue(initial.canShow(channel.copy(status = "verified")))
        assertFalse(initial.canShow(channel.copy(streams = emptyList())))
        assertFalse(initial.probeFinished(channel, ProbeResult.UNCERTAIN, now, timings).canShow(channel))
        val positive = initial.probeFinished(channel, ProbeResult.PLAYABLE, now, timings)
        assertTrue(positive.canShow(channel))
        assertTrue(positive.nextCheckAt < now + day)
        val uncertain = positive.probeFinished(channel, ProbeResult.UNCERTAIN, now + day, timings)
        assertTrue(uncertain.canShow(channel))
        assertTrue(uncertain.playbackExhausted(channel, false, now + day, timings).canShow(channel))
        val hidden = positive.probeFinished(channel, ProbeResult.FAILED, now + day, timings)
        assertFalse(hidden.canShow(channel.copy(status = "verified")))
        val waiting = hidden.probeFinished(channel, ProbeResult.UNCERTAIN, now + 2 * day, timings)
        assertFalse(waiting.canShow(channel))
        val recovered = waiting.probeFinished(channel, ProbeResult.PLAYABLE, now + 3 * day, timings)
        assertTrue(recovered.canShow(channel))
        assertEquals(0, recovered.failedChecks)
    }

    @Test fun updateTagsAreComparedNumericallyAndOnlyNewerVersionsQualify() {
        assertEquals("v0.10.0", ReleaseCheck.newerTag(
            "0.2.2", sequenceOf("v0.2.4", "v0.2.11", "v0.10.0", "v0.9.8", "v5.0.0-beta", "v0.2.2")
        ))
        assertNull(ReleaseCheck.newerTag("0.2.2", sequenceOf("v0.2.1", "v0.2.2", "garbage", "v2147483648.0.0")))
    }

    @Test fun catalogueChangesKeepViewingButInvalidateOldFailuresAndSources() {
        val channel = station("test")
        val watched = ViewingHistory(sessions = 2, lastWatchedAt = now, recentWeight = 5.0)
        val cached = HealthRecord(sourceSet = channel.sourceSet, availability = Availability.UNAVAILABLE,
            lastSource = backup.key, failedChecks = 4, viewing = watched)
        assertEquals(listOf(backup, first), cached.order(channel.streams))
        val changed = channel.copy(streams = listOf(first)).let { cached.trackedFor(it, now) }
        assertEquals(Availability.CHECKING, changed.availability)
        assertEquals(0, changed.failedChecks)
        assertEquals(watched, changed.viewing)
        assertNull(changed.lastSource)
    }

    @Test fun recentlyWatchedOutranksLifetimeCountsAndFairSlotIncludesRareChannels() {
        val recent = ScanCandidate(station("recent"), HealthRecord(checkedAt = now - day,
            viewing = ViewingHistory(sessions = 5, lastWatchedAt = now - 500, recentWeight = 9.0)))
        val oldFavorite = ScanCandidate(station("old"), HealthRecord(checkedAt = now - 10 * day,
            viewing = ViewingHistory(sessions = 600, lastWatchedAt = now - 180 * day, recentWeight = 70.0)))
        val unknown = ScanCandidate(station("unused"), HealthRecord())
        val group = listOf(oldFavorite, unknown, recent)
        assertEquals(recent, ScanPlanner.pick(group, now, useFairSlot = false))
        assertEquals(unknown, ScanPlanner.pick(group, now, useFairSlot = true))
        val recovery = recent.history.probeFinished(recent.channel, ProbeResult.FAILED, now, timings)
        val rareRecovery = unknown.history.probeFinished(unknown.channel, ProbeResult.FAILED, now, timings)
        assertTrue(recovery.nextCheckAt < rareRecovery.nextCheckAt)
        assertTrue(rareRecovery.probeFinished(unknown.channel, ProbeResult.FAILED, now, timings).nextCheckAt > rareRecovery.nextCheckAt)
    }

    @Test fun playbackCountsOnlyQualifiedTimeAndOnlyOneSessionThroughBuffering() {
        val checkpoints = mutableListOf<Pair<Long, Boolean>>()
        val session = ViewingSession { time, counted -> checkpoints.add(time to counted) }
        session.save(20_000, finished = true)
        assertTrue(checkpoints.isEmpty())
        session.start(20_000)
        session.pause(34_000)
        session.save(90_000)
        assertTrue(checkpoints.isEmpty())
        session.start(90_000)
        session.save(106_000)
        session.save(111_000, finished = true)
        assertEquals(listOf(30_000L to true, 5_000L to false), checkpoints)
    }

    @Test fun transportStreamRequiresVideoPayloadRatherThanHttpBytesOrAudio() {
        val packets = ByteArray(188 * 3)
        for (i in 0..2) packets[i * 188] = 0x47
        packets[1] = 0x40
        packets[3] = 0x10
        packets[6] = 1
        packets[7] = 0xc0.toByte()
        assertFalse(MediaSample.transportVideo(packets))
        packets[7] = 0xe0.toByte()
        assertTrue(MediaSample.transportVideo(packets))
        assertFalse(MediaSample.transportVideo("<html>A response is not a video.</html>".toByteArray()))
    }

    private fun station(id: String) = Channel(
        id, id, setOf(Category.NEWS), null, listOf(first, backup), Origin.SATELLITE, "unverified", 0
    )
}
