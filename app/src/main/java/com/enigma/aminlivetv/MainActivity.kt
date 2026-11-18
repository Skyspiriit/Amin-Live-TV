package com.enigma.aminlivetv

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class MainActivity : Activity() {
    private enum class Screen { HOME, GRID, PLAYER, YOUTUBE }

    private lateinit var store: CatalogueStore
    private lateinit var catalogue: Catalogue
    private lateinit var logos: Logos
    private lateinit var history: ChannelHistory
    private lateinit var monitor: ChannelMonitor
    private lateinit var playerView: PlayerView
    private lateinit var rail: LinearLayout
    private lateinit var message: TextView
    private lateinit var channelName: TextView
    private val ui = Handler(Looper.getMainLooper())
    private val updates = Executors.newSingleThreadExecutor()
    private val preferences by lazy { getSharedPreferences("viewing", MODE_PRIVATE) }
    private val hideChannelName = Runnable { channelName.visibility = View.GONE }
    private val fetchUpdates = Runnable { refreshUpdates() }

    private var screen = Screen.HOME
    private var category = Category.DOMESTIC
    private var currentList = emptyList<Channel>()
    private var currentIndex = 0
    private var livePlayer: LivePlayer? = null
    private var youTubePlayer: YouTubePlayer? = null
    private var documentaryControls: View? = null
    private var pendingCatalogue: Catalogue? = null
    private var overlay = false
    private var showRetry = false
    private var latestRelease: String? = null
    private lateinit var appUpdate: AppUpdate
    private var readyUpdate: java.io.File? = null
    private var downloadingRelease: String? = null
    private var nextCatalogueCheck = 0L
    private var nextReleaseCheck = 0L
    private var foreground = false
    private val watching get() = screen == Screen.PLAYER || screen == Screen.YOUTUBE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immersive()
        store = CatalogueStore(this)
        logos = Logos()
        appUpdate = AppUpdate(this) { runOnUiThread { latestRelease?.let(::showUpdateFailed) } }
        val timings = ScanTimings(BuildConfig.TEST_CATALOG)
        history = ChannelHistory(this, timings)
        monitor = ChannelMonitor(this, history, timings, ::onVisibilityChanged)
        catalogue = store.load()
        category = savedCategory("category") ?: Category.DOMESTIC
        showHome()
        appUpdate.handle(intent)
        if (BuildConfig.TEST_CATALOG) Log.i("AminMonitor", "Local home rendered")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        appUpdate.handle(intent)
    }

    private fun scheduleUpdates() {
        ui.removeCallbacks(fetchUpdates)
        if (foreground && !watching) {
            val next = minOf(nextCatalogueCheck, nextReleaseCheck)
            ui.postDelayed(fetchUpdates, (next - SystemClock.elapsedRealtime()).coerceAtLeast(700L))
        }
    }

    private fun refreshUpdates() {
        if (!foreground || watching) return
        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed >= nextCatalogueCheck) {
            nextCatalogueCheck = elapsed + CATALOGUE_INTERVAL
            val current = pendingCatalogue ?: catalogue
            updates.execute {
                val result = store.refresh(current)
                if (result != null) runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    pendingCatalogue = result
                    if (foreground) showPendingCatalogue()
                }
            }
        }
        if (elapsed >= nextReleaseCheck) {
            nextReleaseCheck = elapsed + RELEASE_INTERVAL
            updates.execute {
                ReleaseCheck.latest().onSuccess { tag ->
                    runOnUiThread {
                        if (isDestroyed || latestRelease == tag) return@runOnUiThread
                        latestRelease = tag
                        if (tag != null) downloadRelease(tag)
                        if (screen == Screen.HOME) showHome(currentFocus?.tag)
                    }
                }.onFailure {
                    ui.post {
                        if (isDestroyed) return@post
                        nextReleaseCheck = SystemClock.elapsedRealtime() + CATALOGUE_INTERVAL
                        scheduleUpdates()
                    }
                }
            }
        }
        scheduleUpdates()
    }

    private fun showPendingCatalogue() {
        if (pendingCatalogue == null) return
        when (screen) {
            Screen.HOME -> showHome(currentFocus?.tag)
            Screen.GRID -> showGrid(savedPosition())
            Screen.PLAYER, Screen.YOUTUBE -> Unit
        }
    }

    private fun applyPendingCatalogue() {
        pendingCatalogue?.let { catalogue = it }
        pendingCatalogue = null
        monitor.setCatalogue(catalogue)
    }

    private fun onVisibilityChanged(channel: Channel) {
        when (screen) {
            Screen.HOME -> {
                if (channel.id == preferences.getString("watched-channel", null)) showHome(currentFocus?.tag)
            }
            Screen.GRID -> if (channel.belongsTo(category)) showGrid(savedPosition())
            Screen.PLAYER, Screen.YOUTUBE -> Unit
        }
    }

    private fun savedCategory(key: String): Category? =
        Category.entries.firstOrNull { it.key == preferences.getString(key, null) }

    private fun showHome(restoreFocus: Any? = null) {
        screen = Screen.HOME
        applyPendingCatalogue()
        monitor.setEnabled(foreground)
        scheduleUpdates()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val content = column().apply { setPadding(dp(52), dp(26), dp(52), dp(22)) }
        val heading = FrameLayout(this)
        heading.addView(text(getString(R.string.title_fa), 34, bold = true), frame(-1, -2))
        heading.addView(
            text("v${BuildConfig.VERSION_NAME}", 13, MUTED).apply {
                gravity = Gravity.CENTER_VERTICAL
                textDirection = View.TEXT_DIRECTION_LTR
            }, frame(-2, -1, Gravity.END)
        )
        content.addView(heading, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        var categoryFocus: View? = null
        val focusedCategory = restoreFocus as? Category ?: category
        // Two rows of main categories, then one smaller row of documentary groups.
        val (main, groups) = Category.entries.partition { it.main }
        (main.chunked(2) + listOf(groups.filter { catalogue.inCategory(it).isNotEmpty() })).forEach { items ->
            if (items.isEmpty()) return@forEach
            val small = !items.first().main
            val row = LinearLayout(this).apply { layoutDirection = View.LAYOUT_DIRECTION_RTL }
            items.forEachIndexed { index, item ->
                val tile = LinearLayout(this).apply {
                    gravity = Gravity.CENTER
                    // A small tile puts its icon beside the label, unless six share the row.
                    val beside = small && items.size <= 4
                    orientation = if (beside) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                    background = focusSurface()
                    isFocusable = true
                    isClickable = true
                    tag = item
                    contentDescription = getString(item.title)
                    val icon = dp(if (small) 32 else 38)
                    addView(CategoryIcon(this@MainActivity, item), LinearLayout.LayoutParams(icon, icon))
                    val size = when {
                        !small -> 26
                        items.size > 4 -> 21
                        else -> 24
                    }
                    addView(text(getString(item.title), size, bold = true), LinearLayout.LayoutParams(-2, -2).apply {
                        if (beside) marginStart = dp(10) else topMargin = dp(if (small) 2 else 6)
                    })
                    setOnClickListener { openCategory(item) }
                }
                if (item == focusedCategory) categoryFocus = tile
                row.addView(tile, LinearLayout.LayoutParams(0, -1, 1f).apply {
                    if (index < items.size - 1) marginEnd = dp(14)
                })
            }
            content.addView(row, LinearLayout.LayoutParams(-1, 0, if (small) 0.62f else 1f).apply { topMargin = dp(14) })
        }

        val viewingActions = LinearLayout(this)
        val iranianSeries = button(getString(R.string.iranian_series), 24).apply {
            tag = IRANIAN_SERIES_TAG
            setOnClickListener {
                startActivity(Intent(this@MainActivity, IranianSeriesActivity::class.java))
            }
            viewingActions.addView(this, LinearLayout.LayoutParams(0, -1, 1f))
        }

        val lastCategory = savedCategory("watched-category")
        val lastId = preferences.getString("watched-channel", null)
        val now = System.currentTimeMillis()
        val last = catalogue.channels.find {
            it.id == lastId && it.listedInApp && history.isVisible(it, now)
        }
        val resume = last?.let { channel ->
            button(getString(R.string.resume_channel, channel.name), 20).apply {
                tag = RESUME_TAG
                setOnClickListener {
                    val targetCategory = lastCategory?.takeIf(channel::belongsTo)
                        ?: channel.categories.first(channel::belongsTo)
                    selectCategory(targetCategory)
                    currentList = visibleChannels()
                    val index = currentList.indexOfFirst { it.id == channel.id }
                    if (index >= 0) beginPlayback(index) else showGrid(null)
                }
                viewingActions.addView(this, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = dp(14) })
            }
        }
        content.addView(viewingActions, LinearLayout.LayoutParams(-1, dp(53)).apply { topMargin = dp(12) })
        val update = latestRelease?.let { tag ->
            val ready = readyUpdate
            val label = when {
                ready != null -> getString(R.string.update_ready, tag)
                downloadingRelease == tag -> getString(R.string.update_downloading, tag)
                else -> getString(R.string.update_available, tag)
            }
            button(label, 18).apply {
                this.tag = UPDATE_TAG
                setOnClickListener {
                    when {
                        ready != null -> installUpdate(ready)
                        downloadingRelease == null -> downloadRelease(tag)
                    }
                }
                content.addView(this, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) })
            }
        }
        install(content)
        val restored = when (restoreFocus) {
            RESUME_TAG -> resume
            UPDATE_TAG -> update
            IRANIAN_SERIES_TAG -> iranianSeries
            else -> null
        }
        (restored ?: categoryFocus)?.requestFocus()
    }

    // Downloads and checks a newer release in the background; the home button then installs it.
    private fun downloadRelease(tag: String) {
        if (downloadingRelease != null || readyUpdate?.name == "Amin-Live-TV-$tag.apk") return
        downloadingRelease = tag
        readyUpdate = null
        updates.execute {
            val apk = appUpdate.download(tag)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                downloadingRelease = null
                readyUpdate = apk
                if (screen == Screen.HOME) showHome(currentFocus?.tag)
            }
        }
        if (screen == Screen.HOME) showHome(currentFocus?.tag)
    }

    // Installs a verified update, first asking for this copy's install permission if needed.
    private fun installUpdate(apk: java.io.File) {
        if (appUpdate.canInstall()) appUpdate.install(apk)
        else if (!appUpdate.requestPermission(apk, ::startActivity)) latestRelease?.let(::showUpdateFailed)
    }

    // Continues an update after the viewer returns from the install-permission page.
    private fun resumeUpdate() {
        val (apk, allowed) = appUpdate.takePending() ?: return
        if (allowed) appUpdate.install(apk)
        else AlertDialog.Builder(this).setMessage(R.string.update_permission_off)
            .setPositiveButton(android.R.string.ok, null).show()
    }

    private fun showUpdateFailed(tag: String) = openRelease(tag, getString(R.string.update_available, tag))

    private fun openRelease(tag: String, label: String) {
        val page = ReleaseCheck.page(tag)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page)))
        } catch (_: ActivityNotFoundException) {
            AlertDialog.Builder(this).setTitle(label)
                .setMessage(getString(R.string.update_release_link, page))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun selectCategory(selected: Category) {
        category = selected
        preferences.edit().putString("category", category.key).apply()
    }

    private fun visibleChannels(): List<Channel> {
        val now = System.currentTimeMillis()
        return catalogue.inCategory(category).filter {
            it.youtube != null || it.officialLiveUrl != null || history.isVisible(it, now)
        }
    }

    private fun savedPosition(): String? = preferences.getString("position-${category.key}", null)

    private fun openCategory(selected: Category) {
        selectCategory(selected)
        showGrid(savedPosition())
    }

    private fun showGrid(focusId: String?) {
        val previousPosition = currentList.indexOfFirst { it.id == focusId }
        screen = Screen.GRID
        applyPendingCatalogue()
        currentList = visibleChannels()
        monitor.setEnabled(foreground)
        scheduleUpdates()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val content = column().apply { setPadding(dp(46), dp(24), dp(46), dp(22)) }
        content.addView(text(getString(category.title), 30, bold = true), LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(14)
        })
        if (currentList.isEmpty()) {
            content.addView(
                text(getString(R.string.no_channels), 23, MUTED),
                LinearLayout.LayoutParams(-1, 0, 1f)
            )
            install(content)
            return
        }
        val grid = GridView(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            numColumns = GridView.AUTO_FIT
            columnWidth = dp(152)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            horizontalSpacing = dp(12)
            verticalSpacing = dp(12)
            setPadding(dp(3), dp(3), dp(3), dp(5))
            isVerticalScrollBarEnabled = false
            setDrawSelectorOnTop(true)
            selector = surface(Color.TRANSPARENT, ACCENT)
            adapter = ChannelAdapter(this@MainActivity)
            setOnItemClickListener { _, _, position, _ ->
                val channel = currentList[position]
                when {
                    channel.youtube != null -> playYouTube(position)
                    channel.officialLiveUrl != null -> openOfficialLive(channel)
                    else -> beginPlayback(position)
                }
            }
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, item: View?, position: Int, id: Long) {
                    currentList.getOrNull(position)?.let(::rememberPosition)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>) = Unit
            }
        }
        content.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        install(content)
        grid.requestFocus()
        val selected = currentList.indexOfFirst { it.id == focusId }
        grid.setSelection(if (selected >= 0) selected else previousPosition.coerceIn(currentList.indices))
    }

    private fun openOfficialLive(channel: Channel) {
        val url = channel.officialLiveUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            AlertDialog.Builder(this)
                .setMessage(R.string.official_live_no_browser)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private inner class ChannelAdapter(private val context: Context) : BaseAdapter() {
        override fun getCount(): Int = currentList.size
        override fun getItem(position: Int): Channel = currentList[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
            val tile = recycled as? LinearLayout ?: column().apply {
                layoutParams = android.widget.AbsListView.LayoutParams(-1, dp(120))
                gravity = Gravity.CENTER
                background = surface(SURFACE)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                addView(ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(77), dp(41)))
                addView(text("", 18, bold = true).apply { maxLines = 2 }, LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(5)
                })
                addView(text("", 14, MUTED), LinearLayout.LayoutParams(-1, -2))
            }
            val channel = getItem(position)
            val icon = tile.getChildAt(0) as ImageView
            logos.load(channel.logo, icon)
            // Native streams have no tag; official playback says where it comes from.
            val source = when {
                channel.youtube != null -> getString(R.string.source_youtube)
                channel.officialLiveUrl != null -> getString(R.string.source_official_site)
                else -> null
            }
            (tile.getChildAt(1) as TextView).text = channel.name
            (tile.getChildAt(2) as TextView).apply {
                text = source
                visibility = if (source == null) View.GONE else View.VISIBLE
            }
            tile.contentDescription = listOfNotNull(channel.name, source).joinToString("، ")
            return tile
        }
    }

    private fun beginPlayback(index: Int) {
        if (index !in currentList.indices) return
        screen = Screen.PLAYER
        monitor.setEnabled(false)
        scheduleUpdates()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        overlay = false
        val content = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        playerView = PlayerView(this).apply {
            useController = false
            isFocusable = false
            isClickable = false
            setShutterBackgroundColor(Color.BLACK)
        }
        content.addView(playerView, frame(-1, -1))
        channelName = text("", 24, bold = true).apply {
            background = surface(PANEL)
            setPadding(dp(21), dp(9), dp(21), dp(9))
        }
        content.addView(channelName, frame(-2, -2, Gravity.TOP or Gravity.START).apply {
            topMargin = dp(28)
            marginStart = dp(42)
        })
        message = text("", 22).apply {
            background = surface(PANEL)
            setPadding(dp(24), dp(13), dp(24), dp(13))
            visibility = View.GONE
        }
        content.addView(message, frame(-2, -2, Gravity.CENTER))
        rail = column().apply {
            visibility = View.GONE
            background = surface(PANEL)
            setPadding(dp(16), dp(9), dp(16), dp(12))
        }
        content.addView(rail, frame(-1, -2, Gravity.BOTTOM).apply {
            bottomMargin = dp(26)
            leftMargin = dp(42)
            rightMargin = dp(42)
        })
        install(content)
        content.isFocusableInTouchMode = true
        content.requestFocus()
        tune(index)
    }

    // Official YouTube content inside the app. OK pauses, Up and Down change programme, Back
    // returns to the grid.
    private fun playYouTube(index: Int) {
        val selected = currentList.getOrNull(index) ?: return
        val youtube = selected.youtube ?: return
        closePlayer()
        screen = Screen.YOUTUBE
        currentIndex = index
        rememberPosition(selected)
        monitor.setEnabled(false)
        scheduleUpdates()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val content = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val controls = LinearLayout(this).apply {
            visibility = View.GONE
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val pause = button(getString(R.string.series_play_pause), 20).apply {
            isFocusableInTouchMode = true
            setOnClickListener { youTubePlayer?.togglePause() }
        }
        val next = button(getString(R.string.next_documentary), 20).apply {
            setOnClickListener { youTubePlayer?.skipDocumentary(continuePlaying = true) }
        }
        controls.addView(pause, LinearLayout.LayoutParams(dp(140), dp(48)).apply { marginEnd = dp(8) })
        controls.addView(next, LinearLayout.LayoutParams(dp(140), dp(48)))
        documentaryControls = controls
        val player = YouTubePlayer(this, { label ->
            message.visibility = if (label == null) View.GONE else View.VISIBLE
            if (label != null) message.setText(label)
        }, ::openYouTubeApp) {
            controls.visibility = View.VISIBLE
            pause.requestFocus()
        }
        youTubePlayer = player
        content.addView(player.view, frame(-1, -1))
        channelName = text(selected.name, 24, bold = true).apply {
            background = surface(PANEL)
            setPadding(dp(21), dp(9), dp(21), dp(9))
        }
        content.addView(channelName, frame(-2, -2, Gravity.TOP or Gravity.START).apply {
            topMargin = dp(28)
            marginStart = dp(42)
        })
        message = text("", 22).apply {
            background = surface(PANEL)
            setPadding(dp(24), dp(13), dp(24), dp(13))
            visibility = View.GONE
        }
        content.addView(message, frame(-2, -2, Gravity.CENTER))
        content.addView(controls, frame(-2, -2, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(26)
            marginEnd = dp(42)
        })
        install(content)
        content.isFocusableInTouchMode = true
        content.requestFocus()
        ui.postDelayed(hideChannelName, 4000)
        val documentary = if (category in DOCUMENTARY_GROUPS) {
            youtube.playlist?.let { DocumentaryQueue(this, selected.id, it) }
        } else null
        player.play(youtube, persianSubtitles = selected.categories.any { !it.main }, queue = documentary)
    }

    // Opens one specific broadcast or playlist in the official YouTube TV app. Back there returns here.
    private fun openYouTubeApp(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(YOUTUBE_TV)
        Log.i("AminYouTube", "Opening $url in the YouTube app")
        if (intent.resolveActivity(packageManager) == null) return false
        // Coming back from YouTube lands on the grid, not on a player that would hand off again.
        closePlayer()
        showGrid(currentList[currentIndex].id)
        startActivity(intent)
        return true
    }

    private fun stepYouTube(offset: Int) {
        val playable = currentList.indices.filter { currentList[it].youtube != null }
        val position = playable.indexOf(currentIndex)
        if (playable.size < 2 || position < 0) return
        playYouTube(playable[(position + offset).mod(playable.size)])
    }

    private fun tune(index: Int) {
        currentIndex = index
        val selected = currentList[index]
        rememberPosition(selected)
        channelName.text = selected.name
        channelName.visibility = View.VISIBLE
        ui.removeCallbacks(hideChannelName)
        if (overlay) renderRail()
        if (livePlayer == null) {
            livePlayer = LivePlayer(this, playerView, history, ::showPlayerStatus, ::onPlaybackStarted)
        }
        livePlayer?.select(selected)
    }

    private fun showPlayerStatus(label: Int?, retry: Boolean) {
        showRetry = retry
        message.visibility = if (label == null) View.GONE else View.VISIBLE
        if (label != null) message.setText(label)
        if (overlay) renderRail()
    }

    private fun onPlaybackStarted(channel: Channel) {
        preferences.edit()
            .putString("watched-channel", channel.id)
            .putString("watched-category", category.key)
            .apply()
        channelName.visibility = View.VISIBLE
        ui.removeCallbacks(hideChannelName)
        ui.postDelayed(hideChannelName, 2400)
        if (overlay) renderRail()
    }

    private fun rememberPosition(channel: Channel) {
        preferences.edit().putString("position-${category.key}", channel.id).apply()
    }

    private fun renderRail() {
        rail.removeAllViews()
        val caption = if (showRetry) R.string.retry else R.string.select_channel
        rail.addView(text(getString(caption), 17, MUTED), LinearLayout.LayoutParams(-1, -2))
        val items = LinearLayout(this).apply { layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val visible = visiblePlaybackIndices()
        val windowSize = minOf(visible.size, 5)
        val anchor = visible.count { it < currentIndex }
        val first = (anchor - 2).coerceIn(0, visible.size - windowSize)
        for (index in visible.subList(first, first + windowSize)) {
            val selected = index == currentIndex
            val label = text(currentList[index].name, 18, if (selected) INK else MUTED, selected).apply {
                background = if (selected) surface(SELECTED, ACCENT) else surface(SURFACE)
                setPadding(dp(7), dp(10), dp(7), dp(10))
                maxLines = 2
            }
            items.addView(label, LinearLayout.LayoutParams(0, dp(64), 1f).apply {
                setMargins(dp(4), dp(6), dp(4), 0)
            })
        }
        rail.addView(items, LinearLayout.LayoutParams(-1, -2))
    }

    private fun visiblePlaybackIndices(): List<Int> {
        val now = System.currentTimeMillis()
        return currentList.indices.filter { history.isVisible(currentList[it], now) }
    }

    private fun stepChannel(offset: Int) {
        val visible = visiblePlaybackIndices()
        val target = if (offset < 0) visible.lastOrNull { it < currentIndex } ?: visible.lastOrNull()
            else visible.firstOrNull { it > currentIndex } ?: visible.firstOrNull()
        if (target != null && target != currentIndex) tune(target)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isOk = event.keyCode in setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)
        if (screen == Screen.YOUTUBE) {
            if (documentaryControls?.isShown == true) {
                if (isOk) return super.dispatchKeyEvent(event)
                if (event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                        if (event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) youTubePlayer?.skipDocumentary(continuePlaying = true)
                        else youTubePlayer?.togglePause()
                    }
                    return true
                }
            }
            val offset = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> -1
                KeyEvent.KEYCODE_DPAD_DOWN -> 1
                else -> if (isOk) 0 else return super.dispatchKeyEvent(event)
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                if (isOk) youTubePlayer?.togglePause() else stepYouTube(offset)
            }
            return true
        }
        if (!watching) return super.dispatchKeyEvent(event)
        val offset = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> -1
            KeyEvent.KEYCODE_DPAD_DOWN -> 1
            KeyEvent.KEYCODE_DPAD_LEFT -> if (overlay) 1 else 0
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (overlay) -1 else 0
            else -> if (isOk) 0 else return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            if (isOk) {
                if (overlay && showRetry) livePlayer?.retry()
                overlay = !overlay
                rail.visibility = if (overlay) View.VISIBLE else View.GONE
                if (overlay) renderRail()
            } else if (offset != 0) stepChannel(offset)
        }
        return true
    }

    @Deprecated("Used for the Android TV Back key on API 26 and later")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when (screen) {
            Screen.PLAYER, Screen.YOUTUBE -> {
                youTubePlayer?.skipDocumentary(continuePlaying = false)
                closePlayer()
                showGrid(currentList[currentIndex].id)
            }
            Screen.GRID -> showHome()
            Screen.HOME -> super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        showPendingCatalogue()
        monitor.setEnabled(!watching)
        scheduleUpdates()
        if (screen == Screen.PLAYER && livePlayer == null) tune(currentIndex)
        if (screen == Screen.YOUTUBE && youTubePlayer == null) playYouTube(currentIndex)
        resumeUpdate()
    }

    override fun onStop() {
        foreground = false
        ui.removeCallbacks(fetchUpdates)
        monitor.setEnabled(false)
        closePlayer()
        super.onStop()
    }

    override fun onDestroy() {
        updates.shutdownNow()
        monitor.close()
        logos.close()
        super.onDestroy()
    }

    private fun closePlayer() {
        ui.removeCallbacks(hideChannelName)
        livePlayer?.close()
        livePlayer = null
        youTubePlayer?.close()
        youTubePlayer = null
        documentaryControls = null
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun install(content: View) {
        content.layoutDirection = View.LAYOUT_DIRECTION_RTL
        content.setBackgroundColor(if (watching) Color.BLACK else BACKGROUND)
        setContentView(content)
        immersive()
    }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun frame(width: Int, height: Int, gravity: Int = Gravity.CENTER) =
        FrameLayout.LayoutParams(width, height, gravity)

    private fun text(label: String, size: Int, color: Int = INK, bold: Boolean = false) = TextView(this).apply {
        text = label
        textSize = size.toFloat()
        gravity = Gravity.CENTER
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG_RTL
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun button(label: String, size: Int) = text(label, size, bold = true).apply {
        isFocusable = true
        isClickable = true
        background = focusSurface()
    }

    private fun focusSurface() = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), surface(SELECTED, ACCENT))
        addState(intArrayOf(), surface(SURFACE))
    }

    private fun surface(fill: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(12).toFloat()
        if (stroke != null) setStroke(dp(3), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private class CategoryIcon(context: Context, private val category: Category) : View(context) {
        private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT
            style = Paint.Style.STROKE
            strokeWidth = 1.65f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            canvas.scale(width / 32f, height / 32f)
            when (category) {
                Category.DOMESTIC -> {
                    canvas.drawRoundRect(4f, 7f, 28f, 23f, 3f, 3f, pen)
                    canvas.drawLine(11f, 27f, 21f, 27f, pen)
                    canvas.drawLine(16f, 23f, 16f, 27f, pen)
                }
                Category.NEWS -> {
                    canvas.drawRoundRect(6f, 6f, 26f, 26f, 2f, 2f, pen)
                    canvas.drawRect(10f, 10f, 15f, 16f, pen)
                    canvas.drawLine(19f, 11f, 22f, 11f, pen)
                    canvas.drawLine(19f, 15f, 22f, 15f, pen)
                    canvas.drawLine(10f, 21f, 22f, 21f, pen)
                }
                Category.SPORTS -> {
                    canvas.drawCircle(16f, 16f, 10.5f, pen)
                    canvas.drawLine(9f, 9f, 23f, 23f, pen)
                    canvas.drawLine(9f, 23f, 23f, 9f, pen)
                    canvas.drawOval(12f, 5.5f, 20f, 26.5f, pen)
                }
                Category.FILM -> {
                    canvas.drawRoundRect(5f, 11f, 27f, 26f, 2f, 2f, pen)
                    canvas.drawLine(6f, 11f, 24f, 5f, pen)
                    canvas.drawLine(13f, 9f, 11f, 7f, pen)
                    canvas.drawLine(20f, 7f, 18f, 5f, pen)
                    canvas.drawLine(5f, 16f, 27f, 16f, pen)
                }
                Category.DOCUMENTARY -> {
                    canvas.drawRoundRect(4f, 7f, 28f, 25f, 3f, 3f, pen)
                    canvas.drawLine(13f, 11.5f, 13f, 20.5f, pen)
                    canvas.drawLine(13f, 11.5f, 21f, 16f, pen)
                    canvas.drawLine(13f, 20.5f, 21f, 16f, pen)
                }
                Category.WILDLIFE -> {
                    canvas.drawOval(10f, 15f, 22f, 26f, pen)
                    canvas.drawCircle(7f, 12f, 2.5f, pen)
                    canvas.drawCircle(12.5f, 7f, 2.5f, pen)
                    canvas.drawCircle(19.5f, 7f, 2.5f, pen)
                    canvas.drawCircle(25f, 12f, 2.5f, pen)
                }
                Category.NATURE -> {
                    canvas.drawLine(16f, 4f, 7f, 20f, pen)
                    canvas.drawLine(16f, 4f, 25f, 20f, pen)
                    canvas.drawLine(7f, 20f, 25f, 20f, pen)
                    canvas.drawLine(16f, 20f, 16f, 28f, pen)
                }
                Category.SCIENCE -> {
                    canvas.drawCircle(16f, 16f, 6.5f, pen)
                    canvas.drawOval(3f, 12f, 29f, 20f, pen)
                    canvas.drawCircle(25f, 6f, 1.2f, pen)
                }
                Category.MOVIES -> {
                    canvas.drawRoundRect(5f, 6f, 27f, 26f, 2f, 2f, pen)
                    canvas.drawLine(10f, 6f, 10f, 26f, pen)
                    canvas.drawLine(22f, 6f, 22f, 26f, pen)
                    canvas.drawLine(5f, 16f, 10f, 16f, pen)
                    canvas.drawLine(22f, 16f, 27f, 16f, pen)
                }
                Category.SERIES -> {
                    canvas.drawRoundRect(4f, 10f, 24f, 26f, 2f, 2f, pen)
                    canvas.drawLine(8f, 6f, 28f, 6f, pen)
                    canvas.drawLine(28f, 6f, 28f, 22f, pen)
                    canvas.drawLine(11.5f, 14f, 11.5f, 22f, pen)
                    canvas.drawLine(11.5f, 14f, 17.5f, 18f, pen)
                    canvas.drawLine(11.5f, 22f, 17.5f, 18f, pen)
                }
            }
            canvas.restore()
        }
    }

    private companion object {
        val DOCUMENTARY_GROUPS = setOf(Category.DOCUMENTARY, Category.WILDLIFE, Category.NATURE)
        const val RESUME_TAG = "resume"
        const val IRANIAN_SERIES_TAG = "iranian-series"
        const val YOUTUBE_TV = "com.google.android.youtube.tv"
        const val UPDATE_TAG = "release"
        const val CATALOGUE_INTERVAL = 15 * 60_000L
        const val RELEASE_INTERVAL = 6 * 60 * 60_000L
        val BACKGROUND = Color.rgb(13, 22, 25)
        val SURFACE = Color.rgb(26, 40, 44)
        val SELECTED = Color.rgb(46, 68, 62)
        val PANEL = Color.argb(235, 18, 28, 31)
        val INK = Color.rgb(247, 250, 244)
        val MUTED = Color.rgb(177, 197, 191)
        val ACCENT = Color.rgb(193, 241, 163)
    }
}
