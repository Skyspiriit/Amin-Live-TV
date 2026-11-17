package com.enigma.aminlivetv

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors

// Plays official YouTube content through YouTube's own embedded player (IFrame API), so the
// broadcaster's terms and embedding choices apply. A live channel that isn't broadcasting falls
// back to its playlist. Content whose owner blocks embedding opens in the official YouTube app
// at that broadcast or playlist instead. Anything that doesn't start in time is reported as
// unavailable.
internal class YouTubePlayer(
    context: Context,
    private val onStatus: (label: Int?) -> Unit,
    private val openInYouTubeApp: (url: String) -> Boolean
) {
    private val ui = Handler(Looper.getMainLooper())
    private val lookups = Executors.newSingleThreadExecutor()
    private var source: YouTube? = null
    private var usingPlaylist = false
    private var started = false
    private val startTimeout = Runnable { failed() }

    @SuppressLint("SetJavaScriptEnabled")
    val view = WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        isFocusable = false
        isFocusableInTouchMode = false
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        webChromeClient = WebChromeClient()
        webViewClient = WebViewClient()
        addJavascriptInterface(Bridge(), "Amin")
    }

    fun play(youtube: YouTube) {
        source = youtube
        usingPlaylist = youtube.channel == null
        load()
    }

    fun togglePause() = view.evaluateJavascript("toggle()", null)

    fun close() {
        ui.removeCallbacksAndMessages(null)
        lookups.shutdownNow()
        source = null
        view.stopLoading()
        view.loadUrl("about:blank")
        view.destroy()
    }

    private fun load() {
        val youtube = source ?: return
        val vars = JSONObject()
            .put("autoplay", 1).put("controls", 0).put("rel", 0).put("fs", 0).put("disablekb", 1)
            .put("iv_load_policy", 3).put("playsinline", 1).put("hl", "fa").put("origin", ORIGIN)
        val videoId = if (usingPlaylist) {
            vars.put("listType", "playlist").put("list", youtube.playlist)
            ""
        } else {
            vars.put("channel", youtube.channel)
            "live_stream"
        }
        started = false
        view.visibility = View.VISIBLE
        onStatus(R.string.connecting)
        ui.removeCallbacks(startTimeout)
        ui.postDelayed(startTimeout, START_TIMEOUT_MS)
        // The base URL gives YouTube the referrer its embedded player requires.
        view.loadDataWithBaseURL("$ORIGIN/", page(videoId, vars), "text/html", "utf-8", null)
    }

    private fun failed() {
        val youtube = source ?: return
        if (!usingPlaylist && youtube.playlist != null) {
            Log.i("AminYouTube", "Live broadcast unavailable, playing the playlist")
            usingPlaylist = true
            load()
        } else {
            ui.removeCallbacks(startTimeout)
            view.visibility = View.INVISIBLE
            Log.i("AminYouTube", "Programme unavailable")
            onStatus(R.string.programme_unavailable)
        }
    }

    private inner class Bridge {
        @JavascriptInterface fun state(value: Int) = ui.post {
            if (value == PLAYING) {
                if (!started) Log.i("AminYouTube", "Playing")
                started = true
                ui.removeCallbacks(startTimeout)
                onStatus(null)
            }
        }

        @JavascriptInterface fun error(code: Int) = ui.post {
            Log.i("AminYouTube", "Player error $code")
            if (code == 101 || code == 150) openOutside() else failed()
        }
    }

    // Finds the exact watch link the owner publishes: the live broadcast on the channel's live
    // page, or the playlist's newest video from YouTube's playlist feed.
    private fun openOutside() {
        val youtube = source ?: return
        val playlist = usingPlaylist
        ui.removeCallbacks(startTimeout)
        lookups.execute {
            val url = runCatching {
                val live = if (playlist) null else {
                    val page = fetch("https://www.youtube.com/channel/${youtube.channel}/live")
                    // An upcoming or ended broadcast also has a watch link; only a live one counts.
                    if ("\"isLive\":true" !in page || "\"isUpcoming\":true" in page) null
                    else LIVE_VIDEO.find(page)?.let { "https://www.youtube.com/watch?v=${it.groupValues[1]}" }
                }
                live ?: youtube.playlist?.let { list ->
                    val feed = fetch("https://www.youtube.com/feeds/videos.xml?playlist_id=$list")
                    FEED_VIDEO.find(feed)?.let { "https://www.youtube.com/watch?v=${it.groupValues[1]}&list=$list" }
                }
            }.getOrNull()
            ui.post {
                if (source !== youtube) return@post
                if (url != null && openInYouTubeApp(url)) onStatus(null) else failed()
            }
        }
    }

    private fun fetch(address: String): String {
        val connection = URI(address).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept-Language", "en")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36")
            // Declines YouTube's optional cookies, which otherwise replaces the page with a consent form.
            connection.setRequestProperty("Cookie", "SOCS=CAI")
            return connection.inputStream.use { it.readLimited(4 * 1024 * 1024) }.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun page(videoId: String, vars: JSONObject) = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{width:100%;height:100%}</style>
        </head><body><div id="p"></div>
        <script>
        var player;
        function onYouTubeIframeAPIReady() {
          player = new YT.Player('p', {width: '100%', height: '100%', videoId: ${JSONObject.quote(videoId)},
            playerVars: $vars,
            events: {
              onReady: function (e) { e.target.playVideo(); },
              onStateChange: function (e) { Amin.state(e.data); },
              onError: function (e) { Amin.error(e.data); }
            }});
        }
        function toggle() {
          if (!player || !player.getPlayerState) return;
          if (player.getPlayerState() == 1) player.pauseVideo(); else player.playVideo();
        }
        </script>
        <script src="https://www.youtube.com/iframe_api"></script>
        </body></html>
    """.trimIndent()

    private companion object {
        const val ORIGIN = "https://com.enigma.aminlivetv"
        const val PLAYING = 1
        const val START_TIMEOUT_MS = 25_000L
        val LIVE_VIDEO = Regex("""<link rel="canonical" href="https://www\.youtube\.com/watch\?v=([A-Za-z0-9_-]{11})"""")
        val FEED_VIDEO = Regex("<yt:videoId>([A-Za-z0-9_-]{11})</yt:videoId>")
    }
}
