package com.enigma.aminlivetv

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
internal class LivePlayer(
    context: Context,
    private val view: PlayerView,
    private val history: ChannelHistory,
    private val onStatus: (Int?, Boolean) -> Unit,
    private val onStarted: (Channel) -> Unit
) : Player.Listener, AutoCloseable {

    private val handler = Handler(Looper.getMainLooper())
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val http = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(5000)
        .setReadTimeoutMs(6000)
        .setUserAgent("Amin Live TV/${BuildConfig.VERSION_NAME}")
    private val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(http).setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(1))
        )
        .build()
    private var channel: Channel? = null
    private var orderedSources = emptyList<Stream>()
    private var session: ViewingSession? = null
    private var attemptNetwork: Network? = null
    private var nextSource = 0
    private var allFailuresConfirmed = true
    private var attempting = false
    private var waitingForNetwork = false
    private var closed = false
    private var firstFrame = false
    private var started = false
    private var lastProgress = 0L
    private var lastPosition = 0L
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = postConnectivity()
        override fun onLost(network: Network) = postConnectivity()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            postConnectivity()
    }
    private val watchdog = object : Runnable {
        override fun run() {
            if (!attempting) return
            if (connectivity.internetNetwork() == null) {
                waitForNetwork()
                return
            }
            val now = SystemClock.elapsedRealtime()
            session?.save(now)
            val position = player.currentPosition
            if (started && player.isPlaying && position != lastPosition) lastProgress = now
            lastPosition = position
            val timeout = if (started) STALL_MS else STARTUP_MS
            if (now - lastProgress >= timeout) {
                advance(confirmed = false)
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    init {
        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setHandleAudioBecomingNoisy(true)
        player.addListener(this)
        view.player = player
        connectivity.registerDefaultNetworkCallback(networkCallback)
    }

    fun select(selected: Channel) {
        finishSession()
        channel = selected
        orderedSources = history.forChannel(selected, System.currentTimeMillis()).order(selected.streams)
        session = ViewingSession { playedMs, newSession ->
            history.viewed(selected, playedMs, newSession, System.currentTimeMillis())
        }
        nextSource = 0
        allFailuresConfirmed = true
        attempt()
    }

    fun retry() {
        channel?.let(::select)
    }

    private fun attempt() {
        resetAttempt()
        val selected = channel ?: return
        if (selected.streams.isEmpty()) {
            onStatus(R.string.no_source, false)
            return
        }
        attemptNetwork = connectivity.internetNetwork()
        if (attemptNetwork == null) {
            waitForNetwork()
            return
        }
        waitingForNetwork = false
        val stream = orderedSources.getOrNull(nextSource)
        if (stream == null) {
            history.exhausted(selected, allFailuresConfirmed, System.currentTimeMillis())
            onStatus(R.string.all_sources_failed, true)
            return
        }

        onStatus(if (nextSource == 0) R.string.connecting else R.string.switching_source, false)
        attempting = true
        firstFrame = false
        started = false
        lastPosition = 0L
        lastProgress = SystemClock.elapsedRealtime()
        val mime = when (stream.format) {
            "hls" -> MimeTypes.APPLICATION_M3U8
            "dash" -> MimeTypes.APPLICATION_MPD
            else -> null
        }
        player.setMediaItem(
            MediaItem.Builder()
                .setMediaId("${selected.id}:$nextSource")
                .setUri(stream.url)
                .setMimeType(mime)
                .build()
        )
        player.prepare()
        player.play()
        handler.postDelayed(watchdog, 1000)
    }

    override fun onRenderedFirstFrame() {
        if (!attempting) return
        firstFrame = true
        confirmPlaying()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (isPlaying && attempting) {
            if (started) session?.start(now) else confirmPlaying()
        } else {
            session?.pause(now)
            session?.save(now)
        }
    }

    private fun confirmPlaying() {
        if (!attempting || started || !firstFrame || !player.isPlaying) return
        started = true
        lastProgress = SystemClock.elapsedRealtime()
        session?.start(lastProgress)
        onStatus(null, false)
        channel?.let {
            val source = orderedSources[nextSource]
            history.played(it, source, System.currentTimeMillis())
            Log.i("AminPlayback", "Playing ${it.id} on source ${source.priority}")
            onStarted(it)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        if (!attempting) return
        val source = orderedSources.getOrNull(nextSource) ?: return
        Log.w("AminPlayback", "Source ${source.priority} failed for ${channel?.id} (${error.errorCodeName})")
        val response = error.cause as? HttpDataSource.InvalidResponseCodeException
        val rejected = response != null && response.dataSpec.uri.toString() == source.url &&
            response.responseCode in setOf(401, 403, 404, 410, 451)
        val malformed = error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ||
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED
        advance(confirmed = !started && (rejected || malformed))
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (attempting && playbackState == Player.STATE_ENDED) advance(confirmed = false)
    }

    private fun advance(confirmed: Boolean) {
        val network = connectivity.internetNetwork()
        if (network == null) {
            waitForNetwork()
            return
        }
        if (network != attemptNetwork) {
            attempt()
            return
        }
        channel?.let { history.sourceFailed(it, System.currentTimeMillis()) }
        allFailuresConfirmed = allFailuresConfirmed && confirmed
        resetAttempt()
        nextSource += 1
        onStatus(R.string.switching_source, false)
        handler.postDelayed(::attempt, 500L * nextSource.coerceAtMost(3))
    }

    private fun waitForNetwork() {
        resetAttempt()
        waitingForNetwork = true
        onStatus(R.string.offline, true)
    }

    private fun postConnectivity() {
        handler.post {
            if (closed) return@post
            val network = connectivity.internetNetwork()
            if (network == null && attempting) waitForNetwork()
            if (network != null && (waitingForNetwork || (attempting && network != attemptNetwork))) attempt()
        }
    }

    private fun finishSession() {
        session?.save(SystemClock.elapsedRealtime(), finished = true)
        session = null
    }

    private fun resetAttempt() {
        val now = SystemClock.elapsedRealtime()
        session?.pause(now)
        session?.save(now)
        attempting = false
        waitingForNetwork = false
        handler.removeCallbacksAndMessages(null)
        player.stop()
        player.clearMediaItems()
    }

    override fun close() {
        if (closed) return
        closed = true
        attempting = false
        finishSession()
        handler.removeCallbacksAndMessages(null)
        connectivity.unregisterNetworkCallback(networkCallback)
        player.removeListener(this)
        view.player = null
        player.release()
    }

    private companion object {
        const val STARTUP_MS = 16_000L
        const val STALL_MS = 24_000L
    }
}
