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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
internal class LivePlayer(
    context: Context,
    private val view: PlayerView,
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
    private var nextSource = 0
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
            if (!online()) {
                waitForNetwork()
                return
            }
            val now = SystemClock.elapsedRealtime()
            val position = player.currentPosition
            if (started && player.isPlaying && position != lastPosition) lastProgress = now
            lastPosition = position
            val timeout = if (started) STALL_MS else STARTUP_MS
            if (now - lastProgress >= timeout) {
                advance()
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
        channel = selected
        nextSource = 0
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
        if (!online()) {
            waitForNetwork()
            return
        }
        waitingForNetwork = false
        val stream = selected.streams.getOrNull(nextSource)
        if (stream == null) {
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
        if (isPlaying) confirmPlaying()
    }

    private fun confirmPlaying() {
        if (!attempting || started || !firstFrame || !player.isPlaying) return
        started = true
        lastProgress = SystemClock.elapsedRealtime()
        onStatus(null, false)
        channel?.let {
            Log.i("AminPlayback", "Playing ${it.id} on source ${nextSource + 1}")
            onStarted(it)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        if (!attempting) return
        Log.w("AminPlayback", "Source ${nextSource + 1} failed for ${channel?.id} (${error.errorCodeName})")
        advance()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (attempting && playbackState == Player.STATE_ENDED) advance()
    }

    private fun advance() {
        if (!online()) {
            waitForNetwork()
            return
        }
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
            if (online() && waitingForNetwork) attempt()
            if (!online() && attempting) waitForNetwork()
        }
    }

    private fun online(): Boolean {
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            (BuildConfig.TEST_CATALOG || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }

    private fun resetAttempt() {
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
