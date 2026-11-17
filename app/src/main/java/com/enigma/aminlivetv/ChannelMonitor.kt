package com.enigma.aminlivetv

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.net.URI
import java.util.ArrayDeque
import java.util.concurrent.Executors

internal class ChannelMonitor(
    context: Context,
    private val history: ChannelHistory,
    private val timings: ScanTimings
) : AutoCloseable {
    private class Check(val network: Network) {
        @Volatile var cancelled = false
    }

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val ui = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val starts = ArrayDeque<Long>()
    private val tick = Runnable { schedule() }
    private var channels = emptyList<Channel>()
    private var active: Check? = null
    private var enabled = false
    private var closed = false
    private var nextStart = 0L
    private var fairSlot = 0

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = networkChanged()
        override fun onLost(network: Network) = networkChanged()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = networkChanged()
    }

    fun setCatalogue(catalogue: Catalogue) {
        val eligible = catalogue.channels.filter { channel ->
            channel.listedInApp && channel.streams.any(::scanable)
        }
        if (eligible == channels) return
        channels = eligible
        active?.cancelled = true
        schedule()
    }

    fun setEnabled(value: Boolean) {
        if (closed || value == enabled) return
        enabled = value
        if (enabled) {
            nextStart = maxOf(nextStart, SystemClock.elapsedRealtime() + timings.initialDelayMs)
            connectivity.registerDefaultNetworkCallback(networkCallback)
            schedule()
        } else {
            connectivity.unregisterNetworkCallback(networkCallback)
            ui.removeCallbacks(tick)
            active?.cancelled = true
        }
    }

    private fun networkChanged() {
        ui.post {
            if (!enabled || closed) return@post
            if (active?.network != connectivity.internetNetwork()) active?.cancelled = true
            schedule()
        }
    }

    private fun schedule() {
        ui.removeCallbacks(tick)
        if (!enabled || closed || active != null || channels.isEmpty()) return
        val network = connectivity.internetNetwork() ?: return
        val elapsed = SystemClock.elapsedRealtime()
        val now = System.currentTimeMillis()
        while (starts.isNotEmpty() && elapsed - starts.first >= timings.windowMs) starts.removeFirst()
        val budgetAvailableAt = if (starts.size >= timings.limit) starts.first + timings.windowMs else elapsed
        val candidates = channels.map { ScanCandidate(it, history.forChannel(it, now)) }
        val dueIn = candidates.minOf { (it.history.nextCheckAt - now).coerceAtLeast(0) }
        val delay = maxOf(nextStart - elapsed, budgetAvailableAt - elapsed, dueIn)
        if (delay > 0) {
            ui.postDelayed(tick, delay)
            return
        }
        val candidate = ScanPlanner.pick(candidates, now, useFairSlot = fairSlot == 2) ?: return
        fairSlot = (fairSlot + 1) % 3
        starts.addLast(elapsed)
        nextStart = elapsed + timings.gapMs
        val check = Check(network)
        active = check
        val ordered = candidate.history.order(candidate.channel.streams).filter(::scanable)
        if (BuildConfig.TEST_CATALOG) Log.i("AminMonitor", "Checking ${candidate.channel.id}")
        worker.execute {
            val result = runCatching { StreamProbe { check.cancelled }.check(ordered) }
                .getOrDefault(ProbeResult.UNCERTAIN)
            ui.post {
                active = null
                if (!check.cancelled && enabled && !closed && connectivity.internetNetwork() == check.network) {
                    history.probeFinished(candidate.channel, result, System.currentTimeMillis())
                    if (BuildConfig.TEST_CATALOG) Log.i("AminMonitor", "${candidate.channel.id} $result")
                }
                schedule()
            }
        }
    }

    private fun scanable(stream: Stream): Boolean =
        !BuildConfig.TEST_CATALOG || URI(stream.url).host == "127.0.0.1"

    override fun close() {
        setEnabled(false)
        closed = true
        ui.removeCallbacksAndMessages(null)
        worker.shutdownNow()
    }
}

internal fun ConnectivityManager.internetNetwork(): Network? {
    val network = activeNetwork ?: return null
    val capabilities = getNetworkCapabilities(network) ?: return null
    return network.takeIf {
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (BuildConfig.TEST_CATALOG || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }
}
