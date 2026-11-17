package com.enigma.aminlivetv

import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ParserException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

@OptIn(UnstableApi::class)
internal class StreamProbe(private val cancelled: () -> Boolean) {
    private class Rejected : IOException()
    private data class Sample(val uri: URI, val bytes: ByteArray)

    fun check(sources: List<Stream>): ProbeResult {
        val deadline = SystemClock.elapsedRealtime() + CHANNEL_TIMEOUT_MS
        var allFailed = sources.isNotEmpty()
        for (source in sources) {
            if (cancelled() || SystemClock.elapsedRealtime() >= deadline) return ProbeResult.UNCERTAIN
            val sourceDeadline = minOf(deadline, SystemClock.elapsedRealtime() + SOURCE_TIMEOUT_MS)
            val result = try {
                val uri = URI(source.url)
                when (source.format) {
                    "hls" -> hls(uri, sourceDeadline)
                    "dash" -> dash(uri, sourceDeadline)
                    else -> {
                        val sample = read(uri, sourceDeadline, SAMPLE_BYTES, 0).bytes
                        if (MediaSample.progressiveVideo(sample)) ProbeResult.PLAYABLE else ProbeResult.UNCERTAIN
                    }
                }
            } catch (_: Rejected) {
                ProbeResult.FAILED
            } catch (_: ParserException) {
                ProbeResult.FAILED
            } catch (_: XmlPullParserException) {
                ProbeResult.FAILED
            } catch (_: IOException) {
                ProbeResult.UNCERTAIN
            } catch (_: IllegalArgumentException) {
                ProbeResult.UNCERTAIN
            }
            if (result == ProbeResult.PLAYABLE) return result
            allFailed = allFailed && result == ProbeResult.FAILED
        }
        return if (allFailed) ProbeResult.FAILED else ProbeResult.UNCERTAIN
    }

    private fun hls(uri: URI, deadline: Long): ProbeResult {
        fun parse(sample: Sample) =
            HlsPlaylistParser().parse(Uri.parse(sample.uri.toString()), sample.bytes.inputStream())

        val root = read(uri, deadline, MANIFEST_BYTES)
        return when (val parsed = parse(root)) {
            is HlsMediaPlaylist -> checkHlsMedia(parsed, deadline)
            is HlsMultivariantPlaylist -> {
                val variants = parsed.variants
                    .sortedWith(compareBy({ it.format.height <= 0 }, { it.format.bitrate }))
                    .take(2)
                for (variant in variants) {
                    val result = try {
                        val media = parse(read(URI(variant.url.toString()), deadline, MANIFEST_BYTES)) as? HlsMediaPlaylist
                        media?.let { checkHlsMedia(it, deadline) }
                    } catch (_: IOException) {
                        // An invalid or expired rendition is inconclusive for the whole source.
                        null
                    }
                    if (result == ProbeResult.PLAYABLE) return result
                }
                ProbeResult.UNCERTAIN
            }
            else -> ProbeResult.UNCERTAIN
        }
    }

    private fun checkHlsMedia(playlist: HlsMediaPlaylist, deadline: Long): ProbeResult {
        if (playlist.protectionSchemes != null) return ProbeResult.UNCERTAIN
        val segments = playlist.segments.takeLast(4).filterNot { it.hasGapTag }
        val segment = segments.getOrNull(segments.lastIndex - 1) ?: segments.lastOrNull()
            ?: return ProbeResult.UNCERTAIN
        if (segment.drmInitData != null || segment.fullSegmentEncryptionKeyUri != null) return ProbeResult.UNCERTAIN
        val base = URI(playlist.baseUri)
        try {
            val media = read(base.resolve(segment.url), deadline, SAMPLE_BYTES, segment.byteRangeOffset, segment.byteRangeLength).bytes
            if (MediaSample.transportVideo(media)) return ProbeResult.PLAYABLE
            val init = segment.initializationSegment ?: return ProbeResult.UNCERTAIN
            if (!MediaSample.fragment(media)) return ProbeResult.UNCERTAIN
            val head = read(base.resolve(init.url), deadline, SAMPLE_BYTES, init.byteRangeOffset, init.byteRangeLength).bytes
            return if (MediaSample.videoTrack(head)) ProbeResult.PLAYABLE else ProbeResult.UNCERTAIN
        } catch (_: Rejected) {
            // A live segment can have fallen out of the time window.
            return ProbeResult.UNCERTAIN
        }
    }

    private fun dash(uri: URI, deadline: Long): ProbeResult {
        val data = read(uri, deadline, MANIFEST_BYTES)
        if (data.bytes.toString(Charsets.UTF_8).contains("<!DOCTYPE", ignoreCase = true)) return ProbeResult.FAILED
        val manifest = DashManifestParser().parse(Uri.parse(data.uri.toString()), data.bytes.inputStream())
        if (manifest.periodCount == 0) return ProbeResult.UNCERTAIN
        val period = manifest.getPeriod(manifest.periodCount - 1)
        val track = period.adaptationSets.firstOrNull { it.type == C.TRACK_TYPE_VIDEO } ?: return ProbeResult.FAILED
        val representation = track.representations.filter { it.format.drmInitData == null }
            .minByOrNull { it.format.bitrate } ?: return ProbeResult.UNCERTAIN
        val index = representation.index ?: return ProbeResult.UNCERTAIN
        val duration = manifest.getPeriodDurationUs(manifest.periodCount - 1)
        val nowUs = System.currentTimeMillis() * 1000
        val first = index.getFirstAvailableSegmentNum(duration, nowUs)
        val count = index.getAvailableSegmentCount(duration, nowUs)
        val number = when {
            count > 0 -> first + (count - 2).coerceAtLeast(0)
            manifest.dynamic && count < 0 && manifest.availabilityStartTimeMs != C.TIME_UNSET ->
                index.getSegmentNum(
                    ((System.currentTimeMillis() - manifest.availabilityStartTimeMs - period.startMs - 10_000)
                        .coerceAtLeast(0)) * 1000, duration
                ).coerceAtLeast(first)
            else -> return ProbeResult.UNCERTAIN
        }
        val segment = index.getSegmentUrl(number)
        val base = representation.baseUrls.firstOrNull()?.url ?: return ProbeResult.UNCERTAIN
        return try {
            val media = read(URI(segment.resolveUriString(base)), deadline, SAMPLE_BYTES, segment.start, segment.length).bytes
            if (MediaSample.fragment(media) || MediaSample.transportVideo(media)) ProbeResult.PLAYABLE
            else ProbeResult.UNCERTAIN
        } catch (_: Rejected) {
            ProbeResult.UNCERTAIN
        }
    }

    private fun read(uri: URI, deadline: Long, limit: Int, rangeStart: Long? = null, length: Long = -1): Sample {
        var target = uri
        val sizeLimit = if (length > 0) minOf(length, limit.toLong()).toInt() else limit
        repeat(4) {
            val local = BuildConfig.TEST_CATALOG && target.host == "127.0.0.1" && target.scheme == "http"
            require((target.scheme == "https" || local) && target.host != null && target.userInfo == null)
            val connection = target.toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = timeout(deadline)
                connection.readTimeout = timeout(deadline)
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Amin Live TV/${BuildConfig.VERSION_NAME}")
                connection.setRequestProperty("Accept-Encoding", "identity")
                rangeStart?.let { connection.setRequestProperty("Range", "bytes=$it-${it + sizeLimit - 1}") }
                when (connection.responseCode) {
                    301, 302, 303, 307, 308 -> {
                        val next = connection.getHeaderField("Location") ?: throw IOException("Invalid redirect")
                        target = target.resolve(next)
                        return@repeat
                    }
                    401, 403, 404, 410, 451 -> throw Rejected()
                    200, 206 -> Unit
                    else -> throw IOException("Inconclusive response")
                }
                if (rangeStart != null && rangeStart > 0 &&
                    !connection.getHeaderField("Content-Range").orEmpty().startsWith("bytes $rangeStart-")
                ) throw IOException("Range not available")
                val data = connection.inputStream.use { stream ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    val cap = if (rangeStart == null) sizeLimit + 1 else sizeLimit
                    while (output.size() < cap) {
                        timeout(deadline)
                        val n = stream.read(buffer, 0, minOf(buffer.size, cap - output.size()))
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                    }
                    output.toByteArray()
                }
                if (rangeStart == null && data.size > limit) throw IOException("Large manifest")
                return Sample(target, data)
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    private fun timeout(deadline: Long): Int {
        val remaining = deadline - SystemClock.elapsedRealtime()
        if (remaining <= 0 || cancelled()) throw IOException("Check cancelled or timed out")
        return minOf(remaining, 2200L).toInt()
    }

    private companion object {
        const val CHANNEL_TIMEOUT_MS = 11_000L
        const val SOURCE_TIMEOUT_MS = 5500L
        const val MANIFEST_BYTES = 128 * 1024
        const val SAMPLE_BYTES = 24 * 1024
    }
}
