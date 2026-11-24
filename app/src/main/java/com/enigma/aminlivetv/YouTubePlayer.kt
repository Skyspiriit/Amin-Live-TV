package com.enigma.aminlivetv

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors

// Plays official YouTube content through YouTube's own embedded player (IFrame API), so the
// broadcaster's terms and embedding choices apply. A live channel that isn't broadcasting falls
// back to its playlist. Content whose owner blocks embedding opens in the official YouTube app
// at that broadcast or playlist instead. Anything that doesn't start in time is reported as
// unavailable. Documentaries, films and programme playlists can show Persian subtitles: the video's own Persian
// track, or YouTube's automatic translation of its captions (human-written or automatic), drawn
// by YouTube's player with the original timing.
internal class YouTubePlayer(
    context: Context,
    private val onStatus: (label: Int?) -> Unit,
    private val openInYouTubeApp: (url: String) -> Boolean,
    private val onDocumentaryMode: () -> Unit = {}
) {
    private val ui = Handler(Looper.getMainLooper())
    private val lookups = Executors.newSingleThreadExecutor()
    private var source: YouTube? = null
    private var episode: Episode? = null
    private var curatedVideoId: String? = null
    private var initialSeconds = 0
    private var usingPlaylist = false
    private var subtitles = false
    @Volatile private var suppressEpisodeCaptions = false
    private var started = false
    private var pageId = 0
    private var documentary: DocumentaryQueue? = null
    private var documentaryVideo: String? = null
    private var discovering = false
    private val failedDocumentaries = HashSet<String>()
    private var consecutiveTimeouts = 0
    private val loggedCaptions = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
    private val startTimeout = Runnable { failed(timedOut = true) }

    init {
        // Debug builds can be inspected from a computer (chrome://inspect); release builds can't.
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
    }

    @SuppressLint("SetJavaScriptEnabled")
    val view = WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        isFocusable = false
        isFocusableInTouchMode = false
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url
                if (url.host == "www.youtube.com" && url.path == "/api/timedtext") {
                    // The embedded player can reuse a caption preference from YouTube. Suppress
                    // its optional captions for this episode until the viewer opts in here.
                    if (suppressEpisodeCaptions) {
                        return WebResourceResponse("text/plain", "utf-8", 204, "No Content", emptyMap(),
                            ByteArrayInputStream(byteArrayOf()))
                    }
                    captionsLoading(url)
                }
                return null
            }
        }
        addJavascriptInterface(Bridge(), "Amin")
    }

    fun play(youtube: YouTube, persianSubtitles: Boolean = false, queue: DocumentaryQueue? = null) {
        source = youtube
        curatedVideoId = youtube.videos.firstOrNull()
        subtitles = persianSubtitles
        usingPlaylist = youtube.channel == null
        documentary = queue
        if (usingPlaylist && queue != null) beginDocumentaries() else load()
    }

    fun playEpisode(selected: Episode, seconds: Double, captions: Boolean) {
        episode = selected
        initialSeconds = seconds.toInt()
        subtitles = false
        suppressEpisodeCaptions = !captions
        load()
    }

    fun togglePause() = view.evaluateJavascript("toggle()", null)

    fun skipDocumentary(continuePlaying: Boolean) {
        if (!usingPlaylist || documentary == null || discovering) return
        if (documentaryVideo != null) {
            advanceDocumentary(DocumentaryResult.SKIPPED)
        } else if (continuePlaying) {
            failedDocumentaries.clear()
        }
        if (continuePlaying) {
            consecutiveTimeouts = 0
            playNextDocumentary()
        }
    }

    fun pauseEpisode() = view.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo()", null)

    fun sampleEpisode(callback: (EpisodePlayback?) -> Unit) {
        view.evaluateJavascript("episodeProgress()") { value ->
            val sample = runCatching {
                val json = JSONObject(value)
                EpisodePlayback(json.getString("videoId"), json.getInt("state"),
                    json.getDouble("seconds"), json.getDouble("duration"))
            }.getOrNull()
            callback(sample)
        }
    }

    fun close() {
        ++pageId
        ui.removeCallbacksAndMessages(null)
        lookups.shutdownNow()
        source = null
        episode = null
        documentary = null
        view.stopLoading()
        view.loadUrl("about:blank")
        view.destroy()
    }

    private fun load() {
        val selected = episode
        val youtube = source
        val vars = JSONObject()
            .put("autoplay", 1).put("controls", 0).put("rel", 0).put("fs", 0).put("disablekb", 1)
            .put("iv_load_policy", 3).put("playsinline", 1).put("hl", "fa").put("origin", ORIGIN)
        if (subtitles) vars.put("cc_load_policy", 1).put("cc_lang_pref", "fa")
        val videoId = if (selected != null) {
            vars.put("controls", 1).put("disablekb", 0).put("start", initialSeconds)
            if (!suppressEpisodeCaptions) vars.put("cc_load_policy", 1)
            view.isFocusable = true
            selected.videoId
        } else if (documentaryVideo != null) {
            documentaryVideo.orEmpty()
        } else if (youtube == null) {
            return
        } else if (usingPlaylist && youtube.videos.isNotEmpty()) {
            // Cue a valid first video; onReady loads the complete reviewed list through the official API.
            vars.put("autoplay", 0)
            youtube.videos.first()
        } else if (usingPlaylist) {
            vars.put("listType", "playlist").put("list", youtube.playlist)
            ""
        } else {
            vars.put("channel", youtube.channel)
            "live_stream"
        }
        started = false
        val generation = ++pageId
        if (discovering) vars.put("autoplay", 0)
        view.visibility = View.VISIBLE
        onStatus(R.string.connecting)
        ui.removeCallbacks(startTimeout)
        ui.postDelayed(startTimeout, if (discovering) PLAYLIST_TIMEOUT_MS else START_TIMEOUT_MS)
        // The base URL gives YouTube the referrer its embedded player requires.
        val origin = if (selected == null) ORIGIN else "https://${view.context.packageName}"
        vars.put("origin", origin)
        if (BuildConfig.DEBUG && documentaryVideo != null) Log.i("AminYouTube", "Documentary $videoId")
        view.loadDataWithBaseURL("$origin/", page(videoId, vars, generation), "text/html", "utf-8", null)
    }

    private fun beginDocumentaries() {
        onDocumentaryMode()
        playNextDocumentary()
    }

    private fun playNextDocumentary() {
        documentaryVideo = documentary?.current
        discovering = documentaryVideo == null
        load()
    }

    private fun usePlaylist(videos: List<String>) {
        discovering = false
        val queue = documentary ?: return
        if (BuildConfig.DEBUG) Log.i("AminYouTube", if (videos.isEmpty()) "Using cached documentary candidates"
            else "Refreshed documentary candidates: ${videos.size}")
        queue.renew(videos, failedDocumentaries)
        if (queue.current == null) documentaryUnavailable() else playNextDocumentary()
    }

    private fun advanceDocumentary(result: DocumentaryResult) {
        val video = documentaryVideo ?: return
        if (result == DocumentaryResult.UNAVAILABLE) failedDocumentaries.add(video)
        documentary?.advance(result)
        documentaryVideo = null
    }

    private fun documentaryUnavailable() {
        ++pageId
        ui.removeCallbacks(startTimeout)
        view.stopLoading()
        view.loadUrl("about:blank")
        view.visibility = View.INVISIBLE
        onStatus(R.string.programme_unavailable)
    }

    // Logs each caption file the player fetches once, e.g. "en (asr) as fa" for YouTube's
    // Persian translation of automatic English captions.
    private fun captionsLoading(url: Uri) {
        val video = url.getQueryParameter("v") ?: return
        val language = url.getQueryParameter("lang") ?: return
        val kind = url.getQueryParameter("kind")?.let { " ($it)" }.orEmpty()
        val shown = url.getQueryParameter("tlang")?.let { " as $it" }.orEmpty()
        val entry = "$video: $language$kind$shown"
        if (loggedCaptions.add(entry)) Log.i("AminYouTube", "Captions loading for $entry")
    }

    private fun failed(timedOut: Boolean = false) {
        if (discovering) {
            usePlaylist(emptyList())
            return
        }
        if (documentaryVideo != null) {
            advanceDocumentary(DocumentaryResult.UNAVAILABLE)
            if (timedOut && ++consecutiveTimeouts >= MAX_CONSECUTIVE_TIMEOUTS) documentaryUnavailable()
            else playNextDocumentary()
            return
        }
        if (episode != null) {
            episodeUnavailable(R.string.programme_unavailable)
            return
        }
        val youtube = source ?: return
        if (!usingPlaylist && youtube.playlist != null) {
            Log.i("AminYouTube", "Live broadcast unavailable, playing the playlist")
            usingPlaylist = true
            if (documentary != null) beginDocumentaries() else load()
        } else {
            ui.removeCallbacks(startTimeout)
            view.visibility = View.INVISIBLE
            Log.i("AminYouTube", "Programme unavailable")
            onStatus(R.string.programme_unavailable)
        }
    }

    private fun episodeUnavailable(label: Int) {
        ui.removeCallbacks(startTimeout)
        view.visibility = View.INVISIBLE
        onStatus(label)
    }

    private inner class Bridge {
        @JavascriptInterface fun state(generation: Int, value: Int, video: String) = ui.post {
            if (generation != pageId || discovering || (documentaryVideo != null && documentaryVideo != video)) return@post
            if (source?.videos?.contains(video) == true) curatedVideoId = video
            if (value == PLAYING) {
                if (!started) Log.i("AminYouTube", "Playing")
                started = true
                consecutiveTimeouts = 0
                ui.removeCallbacks(startTimeout)
                onStatus(null)
            } else if (value == ENDED && started && documentaryVideo != null) {
                advanceDocumentary(DocumentaryResult.WATCHED)
                playNextDocumentary()
            } else if (documentaryVideo != null) {
                if (value == PAUSED) ui.removeCallbacks(startTimeout)
                if (value == BUFFERING) {
                    ui.removeCallbacks(startTimeout)
                    ui.postDelayed(startTimeout, START_TIMEOUT_MS)
                }
            }
        }

        @JavascriptInterface fun playlist(generation: Int, json: String) = ui.post {
            if (generation != pageId || !discovering) return@post
            val videos = runCatching {
                val list = JSONArray(json)
                documentaryCandidates((0 until list.length()).asSequence().map { list.optString(it) })
            }.getOrDefault(emptyList())
            if (videos.isNotEmpty()) usePlaylist(videos)
        }

        @JavascriptInterface fun subtitles(video: String, choice: String) {
            Log.i("AminYouTube", "Subtitles for $video: $choice")
        }

        @JavascriptInterface fun error(generation: Int, code: Int) = ui.post {
            if (generation != pageId) return@post
            Log.i("AminYouTube", "Player error $code")
            if (discovering || documentaryVideo != null) {
                // Playlist metadata can still arrive when the first item in the playlist is blocked.
                if (!discovering) failed()
            } else if (code == 101 || code == 150) {
                if (episode != null) episodeUnavailable(R.string.series_embed_blocked) else openOutside()
            } else failed()
        }
    }

    // Finds the exact watch link the owner publishes: the live broadcast on the channel's live
    // page, or the playlist's newest video from YouTube's playlist feed.
    private fun openOutside() {
        val youtube = source ?: return
        val playlist = usingPlaylist
        val generation = pageId
        val liveOnly = documentary != null
        ui.removeCallbacks(startTimeout)
        lookups.execute {
            val url = runCatching {
                val live = if (playlist) null else {
                    val page = fetch("https://www.youtube.com/channel/${youtube.channel}/live")
                    // An upcoming or ended broadcast also has a watch link; only a live one counts.
                    if ("\"isLive\":true" !in page || "\"isUpcoming\":true" in page) null
                    else LIVE_VIDEO.find(page)?.let { "https://www.youtube.com/watch?v=${it.groupValues[1]}" }
                }
                live ?: curatedVideoId?.takeIf { it in youtube.videos }?.let {
                    "https://www.youtube.com/watch?v=$it"
                } ?: youtube.playlist?.takeUnless { liveOnly }?.let { list ->
                    val feed = fetch("https://www.youtube.com/feeds/videos.xml?playlist_id=$list")
                    FEED_VIDEO.find(feed)?.let { "https://www.youtube.com/watch?v=${it.groupValues[1]}&list=$list" }
                }
            }.getOrNull()
            ui.post {
                if (generation != pageId || source !== youtube) return@post
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

    private fun page(videoId: String, vars: JSONObject, generation: Int) = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{width:100%;height:100%}</style>
        </head><body><div id="p"></div>
        <script>
        var player, subtitles = $subtitles, current = '', rank = 0, checks = 0, retry;
        var discovering = $discovering, readingPlaylist;
        var reviewedVideos = ${JSONArray(if (episode == null && usingPlaylist) source?.videos.orEmpty() else emptyList<String>())};
        var FA = {languageCode: 'fa', languageName: 'فارسی'};
        // Picks the best Persian subtitles YouTube's player can give, for each video in turn:
        // 5 the video's own Persian track, 4 YouTube's translation of human English captions,
        // 3 of other human captions, 2 of the automatic captions the player shows by itself,
        // 1 of automatic English captions asked for directly (the player doesn't list those,
        // and only shows them by itself for some videos). A better choice replaces a worse one.
        // When the player shows automatic Persian captions, the speech is Persian: no subtitles.
        function persian() {
          if (!subtitles || !player || !player.getOption) return;
          var video = (player.getVideoData() || {}).video_id || '';
          if (!video) return;
          if (video != current) { current = video; rank = 0; checks = 0; }
          var tracks = player.getOption('captions', 'tracklist') || [];
          var showing = player.getOption('captions', 'track') || {};
          function find(test) { return tracks.filter(test)[0]; }
          var fa = find(function (t) { return t.languageCode.indexOf('fa') == 0; });
          var en = find(function (t) { return t.languageCode.indexOf('en') == 0 && t.kind != 'asr'; });
          var other = find(function (t) { return t.kind != 'asr' && t.is_translateable; });
          var auto = showing.kind == 'asr' && showing.languageCode ? showing : null;
          var playing = player.getPlayerState() == 1;
          var spokenPersian = auto && auto.languageCode.indexOf('fa') == 0;
          var pick = spokenPersian ? [6, null, 'none needed, persian speech']
            : fa ? [5, {languageCode: fa.languageCode}, 'persian']
            : en ? [4, {languageCode: en.languageCode, translationLanguage: FA}, 'translating ' + en.languageCode]
            : other ? [3, {languageCode: other.languageCode, translationLanguage: FA}, 'translating ' + other.languageCode]
            : auto ? [2, {languageCode: auto.languageCode, kind: 'asr', translationLanguage: FA}, 'translating automatic ' + auto.languageCode]
            : playing ? [1, {languageCode: 'en', kind: 'asr', translationLanguage: FA}, 'asking for automatic en'] : null;
          if (pick && pick[0] > rank) {
            rank = pick[0];
            if (pick[1]) {
              player.loadModule('captions');
              player.setOption('captions', 'track', pick[1]);
              player.setOption('captions', 'fontSize', 1);
            } else {
              player.unloadModule('captions');
            }
            Amin.subtitles(video, pick[2]);
          }
          // Track lists arrive a moment after playback starts and no event announces them, so
          // look again each second for the first 20 seconds of playback.
          if (rank < 4 && checks < 20) {
            if (playing) checks++;
            clearTimeout(retry);
            retry = setTimeout(persian, 1000);
          }
        }
        function readPlaylist() {
          if (!player || !player.getPlaylist) return;
          var items = player.getPlaylist() || [];
          if (items.length) {
            clearInterval(readingPlaylist);
            Amin.playlist($generation, JSON.stringify(items.slice(0, 500)));
          }
        }
        function onYouTubeIframeAPIReady() {
          player = new YT.Player('p', {width: '100%', height: '100%', videoId: ${JSONObject.quote(videoId)},
            playerVars: $vars,
            events: {
              onReady: function (e) {
                if (discovering) {
                  readingPlaylist = setInterval(readPlaylist, 250);
                  readPlaylist();
                  return;
                }
                if (subtitles) e.target.loadModule('captions');
                if (reviewedVideos.length) e.target.loadPlaylist(reviewedVideos, 0, 0);
                else e.target.playVideo();
              },
              onApiChange: persian,
              onStateChange: function (e) {
                if (discovering) return;
                var data = e.target.getVideoData && e.target.getVideoData();
                Amin.state($generation, e.data, (data || {}).video_id || '');
                if (e.data == 1) persian();
              },
              onError: function (e) { Amin.error($generation, e.data); }
            }});
        }
        function toggle() {
          if (!player || !player.getPlayerState) return;
          var state = player.getPlayerState();
          if (state == 1 || (${documentaryVideo != null} && state == 3)) player.pauseVideo();
          else player.playVideo();
        }
        function episodeProgress() {
          if (!player || !player.getVideoUrl || !player.getVideoUrl()) return null;
          try {
            return {videoId: new URL(player.getVideoUrl()).searchParams.get('v'),
              seconds: player.getCurrentTime(), duration: player.getDuration(), state: player.getPlayerState()};
          } catch (e) { return null; }
        }
        </script>
        <script src="https://www.youtube.com/iframe_api"></script>
        </body></html>
    """.trimIndent()

    private companion object {
        const val ORIGIN = "https://com.enigma.aminlivetv"
        const val ENDED = 0
        const val PLAYING = 1
        const val PAUSED = 2
        const val BUFFERING = 3
        const val START_TIMEOUT_MS = 25_000L
        const val PLAYLIST_TIMEOUT_MS = 12_000L
        const val MAX_CONSECUTIVE_TIMEOUTS = 5
        val LIVE_VIDEO = Regex("""<link rel="canonical" href="https://www\.youtube\.com/watch\?v=([A-Za-z0-9_-]{11})"""")
        val FEED_VIDEO = Regex("<yt:videoId>([A-Za-z0-9_-]{11})</yt:videoId>")
    }
}

internal data class EpisodePlayback(val videoId: String, val state: Int, val seconds: Double, val duration: Double)
