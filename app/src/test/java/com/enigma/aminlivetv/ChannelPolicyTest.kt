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

    @Test fun onlyDomesticNewsAndBbcAreListedInNews() {
        val domestic = station("irinn").copy(origin = Origin.DOMESTIC)
        val bbc = station(Channel.BBC_PERSIAN).copy(streams = emptyList(), status = "restricted")
        val international = station("international")
        val multi = station("mixed").copy(categories = setOf(Category.FILM, Category.NEWS))
        val catalogue = Catalogue(2, listOf(domestic, bbc, international, multi))
        assertEquals(listOf(domestic, bbc), catalogue.inCategory(Category.NEWS))
        assertEquals(listOf(multi), catalogue.inCategory(Category.FILM))
        assertFalse(bbc.heldByCatalogue)
        assertFalse(international.listedInApp)
    }

    @Test fun uncertainChecksAndStaleResultsNeverHideAChannel() {
        val channel = station("test")
        val initial = HealthRecord().trackedFor(channel, now)
        val positive = initial.probeFinished(channel, ProbeResult.PLAYABLE, now, timings)
        assertEquals(Availability.AVAILABLE, positive.availabilityAt(now))
        assertEquals(Availability.CHECKING, positive.availabilityAt(now + day))
        val uncertain = positive.probeFinished(channel, ProbeResult.UNCERTAIN, now + day, timings)
        assertEquals(Availability.CHECKING, uncertain.availabilityAt(now + day))
        val hidden = positive.probeFinished(channel, ProbeResult.FAILED, now + day, timings)
        assertEquals(Availability.UNAVAILABLE, hidden.availabilityAt(now + 2 * day))
        val waiting = hidden.probeFinished(channel, ProbeResult.UNCERTAIN, now + 2 * day, timings)
        assertEquals(Availability.UNAVAILABLE, waiting.availabilityAt(now + 2 * day))
        val recovered = waiting.probeFinished(channel, ProbeResult.PLAYABLE, now + 3 * day, timings)
        assertEquals(Availability.AVAILABLE, recovered.availabilityAt(now + 3 * day))
        assertEquals(0, recovered.failedChecks)
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
