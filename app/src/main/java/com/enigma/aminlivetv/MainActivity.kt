package com.enigma.aminlivetv

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private enum class Screen { HOME, GRID, PLAYER }

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
    private val fetchCatalogue = Runnable { refreshCatalogue() }

    private var screen = Screen.HOME
    private var category = Category.DOMESTIC
    private var currentList = emptyList<Channel>()
    private var currentIndex = 0
    private var livePlayer: LivePlayer? = null
    private var pendingCatalogue: Catalogue? = null
    private var overlay = false
    private var showRetry = false
    private var catalogueRequested = false
    private var foreground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immersive()
        store = CatalogueStore(this)
        logos = Logos()
        val timings = ScanTimings(BuildConfig.TEST_CATALOG)
        history = ChannelHistory(this, timings)
        monitor = ChannelMonitor(this, history, timings)
        catalogue = store.load()
        category = savedCategory("category") ?: Category.DOMESTIC
        showHome()
        if (BuildConfig.TEST_CATALOG) Log.i("AminMonitor", "Local home rendered")
    }

    private fun refreshCatalogue() {
        if (catalogueRequested) return
        catalogueRequested = true
        val current = catalogue
        updates.execute {
            val result = store.refresh(current)
            if (result != null) runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                pendingCatalogue = result
                if (screen == Screen.HOME && foreground) showHome(currentFocus?.tag)
            }
        }
    }

    private fun savedCategory(key: String): Category? =
        Category.entries.firstOrNull { it.key == preferences.getString(key, null) }

    private fun showHome(restoreFocus: Any? = null) {
        screen = Screen.HOME
        pendingCatalogue?.let { catalogue = it }
        pendingCatalogue = null
        monitor.setCatalogue(catalogue)
        monitor.setEnabled(foreground)
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
        Category.entries.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { layoutDirection = View.LAYOUT_DIRECTION_RTL }
            pair.forEachIndexed { index, item ->
                val tile = LinearLayout(this).apply {
                    gravity = Gravity.CENTER
                    orientation = LinearLayout.VERTICAL
                    background = focusSurface()
                    isFocusable = true
                    isClickable = true
                    tag = item
                    contentDescription = getString(item.title)
                    addView(CategoryIcon(this@MainActivity, item), LinearLayout.LayoutParams(dp(38), dp(38)))
                    addView(text(getString(item.title), 26, bold = true), LinearLayout.LayoutParams(-1, -2).apply {
                        topMargin = dp(6)
                    })
                    setOnClickListener { openCategory(item) }
                }
                if (item == focusedCategory) categoryFocus = tile
                row.addView(tile, LinearLayout.LayoutParams(0, -1, 1f).apply {
                    if (index == 0) marginEnd = dp(14)
                })
            }
            content.addView(row, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(14) })
        }

        val lastCategory = savedCategory("watched-category")
        val lastId = preferences.getString("watched-channel", null)
        val now = System.currentTimeMillis()
        val last = catalogue.channels.find {
            it.id == lastId && it.listedInApp && it.streams.isNotEmpty() && history.isVisible(it, now)
        }
        val resume = last?.let { channel ->
            button(getString(R.string.resume_channel, channel.name), 20).apply {
                tag = RESUME_TAG
                setOnClickListener {
                    val targetCategory = lastCategory?.takeIf(channel::belongsTo)
                        ?: channel.categories.first(channel::belongsTo)
                    selectCategory(targetCategory)
                    val index = currentList.indexOfFirst { it.id == channel.id }
                    if (index >= 0) beginPlayback(index) else showGrid(null)
                }
                content.addView(this, LinearLayout.LayoutParams(-1, dp(53)).apply { topMargin = dp(17) })
            }
        }
        install(content)
        if (restoreFocus == RESUME_TAG && resume != null) resume.requestFocus() else categoryFocus?.requestFocus()
    }

    private fun selectCategory(selected: Category) {
        category = selected
        val now = System.currentTimeMillis()
        currentList = catalogue.inCategory(selected)
            .filter { history.isVisible(it, now) }
            .sortedBy { it.streams.isEmpty() }
        preferences.edit().putString("category", category.key).apply()
    }

    private fun openCategory(selected: Category) {
        selectCategory(selected)
        showGrid(preferences.getString("position-${category.key}", null))
    }

    private fun showGrid(focusId: String?) {
        screen = Screen.GRID
        monitor.setEnabled(foreground)
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
            setOnItemClickListener { _, _, position, _ -> beginPlayback(position) }
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
        val index = currentList.indexOfFirst { it.id == focusId }.coerceAtLeast(0)
        grid.setSelection(index)
    }

    private inner class ChannelAdapter(private val context: Context) : BaseAdapter() {
        override fun getCount(): Int = currentList.size
        override fun getItem(position: Int): Channel = currentList[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
            val tile = recycled as? LinearLayout ?: column().apply {
                layoutParams = android.widget.AbsListView.LayoutParams(-1, dp(107))
                gravity = Gravity.CENTER
                background = surface(SURFACE)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                addView(ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(77), dp(41)))
                addView(text("", 18, bold = true).apply { maxLines = 2 }, LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(5)
                })
            }
            val channel = getItem(position)
            val icon = tile.getChildAt(0) as ImageView
            logos.load(channel.logo, icon)
            (tile.getChildAt(1) as TextView).apply {
                text = channel.name
                setTextColor(if (channel.streams.isEmpty()) MUTED else INK)
            }
            tile.contentDescription = if (channel.streams.isEmpty()) {
                "${channel.name}، ${getString(R.string.no_source)}"
            } else channel.name
            return tile
        }
    }

    private fun beginPlayback(index: Int) {
        if (index !in currentList.indices) return
        screen = Screen.PLAYER
        monitor.setEnabled(false)
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
    }

    private fun rememberPosition(channel: Channel) {
        preferences.edit().putString("position-${category.key}", channel.id).apply()
    }

    private fun renderRail() {
        rail.removeAllViews()
        val caption = if (showRetry) R.string.retry else R.string.select_channel
        rail.addView(text(getString(caption), 17, MUTED), LinearLayout.LayoutParams(-1, -2))
        val items = LinearLayout(this).apply { layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val windowSize = minOf(currentList.size, 5)
        val first = (currentIndex - 2).coerceIn(0, currentList.size - windowSize)
        for (index in first until first + windowSize) {
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (screen != Screen.PLAYER) return super.dispatchKeyEvent(event)
        val isOk = event.keyCode in setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)
        val offset = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> 1
            KeyEvent.KEYCODE_DPAD_DOWN -> -1
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
            } else if (offset != 0 && currentList.size > 1) {
                tune(Math.floorMod(currentIndex + offset, currentList.size))
            }
        }
        return true
    }

    @Deprecated("Used for the Android TV Back key on API 26 and later")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when (screen) {
            Screen.PLAYER -> {
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
        if (screen == Screen.HOME && pendingCatalogue != null) showHome(currentFocus?.tag)
        monitor.setEnabled(screen != Screen.PLAYER)
        ui.postDelayed(fetchCatalogue, 700)
        if (screen == Screen.PLAYER && livePlayer == null) tune(currentIndex)
    }

    override fun onStop() {
        foreground = false
        ui.removeCallbacks(fetchCatalogue)
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
        content.setBackgroundColor(if (screen == Screen.PLAYER) Color.BLACK else BACKGROUND)
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
            }
            canvas.restore()
        }
    }

    private companion object {
        const val RESUME_TAG = "resume"
        val BACKGROUND = Color.rgb(13, 22, 25)
        val SURFACE = Color.rgb(26, 40, 44)
        val SELECTED = Color.rgb(46, 68, 62)
        val PANEL = Color.argb(235, 18, 28, 31)
        val INK = Color.rgb(247, 250, 244)
        val MUTED = Color.rgb(177, 197, 191)
        val ACCENT = Color.rgb(193, 241, 163)
    }
}
