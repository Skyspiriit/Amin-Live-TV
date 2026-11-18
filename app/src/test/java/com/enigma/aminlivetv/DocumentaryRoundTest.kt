package com.enigma.aminlivetv

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentaryRoundTest {
    private val videos = (1..40).map { it.toString().padStart(11, '0') }

    @Test fun roundsShuffleValidDistinctCandidatesAndCapAtTwentyFive() {
        val excluded = setOf(videos.first())
        val round = newDocumentaryRound(videos + videos + "bad-id", excluded, Random(13)) { 0L }
        assertEquals(25, round.videos.size)
        assertEquals(round.videos.size, round.videos.distinct().size)
        assertTrue(round.videos.all { it in videos })
        assertFalse(round.videos.any { it in excluded })
        assertFalse(round.videos == round.videos.sorted())
    }

    @Test fun unseenVideosComeFirstAndTheOldestWatchedOrSkippedFillTheRound() {
        val seen = videos.take(28).mapIndexed { index, id ->
            id to (index + 1L) * (if (index % 2 == 0) 1 else -1)
        }.toMap()
        val round = newDocumentaryRound(videos, random = Random(4)) { seen[it] ?: 0L }
        assertEquals(videos.drop(28).toSet(), round.videos.take(12).toSet())
        assertEquals(videos.take(13).toSet(), round.videos.drop(12).toSet())
    }

    @Test fun advancementRetainsTheRoundAndExhaustionDoesNotWrap() {
        val candidates = videos.take(2)
        val round = newDocumentaryRound(candidates, random = Random(3)) { 0L }
        val next = round.advance()
        assertEquals(round.videos, next.videos)
        assertEquals(round.videos[1], next.current)
        val exhausted = next.advance()
        assertNull(exhausted.current)
        assertEquals(exhausted, exhausted.advance())
    }
}
