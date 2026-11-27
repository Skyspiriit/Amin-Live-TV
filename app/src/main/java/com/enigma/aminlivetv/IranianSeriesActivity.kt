package com.enigma.aminlivetv

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.concurrent.Executors

class IranianSeriesActivity : Activity() {
    private lateinit var catalogue: Catalogue
    private lateinit var progress: SeriesProgress
    private val ui = Handler(Looper.getMainLooper())
    private val updates = Executors.newSingleThreadExecutor()
    private val thumbnails = Logos()
    private var series: IranianSeries? = null
    private var episode: Episode? = null
    private var player: YouTubePlayer? = null
    private var lastSample: EpisodePlayback? = null
    private var lastSaved: EpisodePlayback? = null
    private var savedAt = 0L
    private var resumeSeconds = 0.0
    private var reachedStart = false
    private var active = false
    private var finished = false
    private var unavailable = false
    private var leavingEpisode = false
    private lateinit var nextButton: TextView
    private val library: List<IranianSeries> get() = catalogue.iranianSeries
    private val sampleTick = object : Runnable {
        override fun run() {
            val current = player ?: return
            if (!active || finished || unavailable) return
            current.sampleEpisode { value ->
                if (player === current && active && !unavailable) {
                    if (value != null) recordSample(value)
                    ui.removeCallbacks(this)
                    if (!finished) ui.postDelayed(this, SAMPLE_INTERVAL)
                }
            }
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        AppTheme.load(this)
        val store = CatalogueStore(this)
        catalogue = store.load()
        progress = SeriesProgress(this)
        showLibrary()
        // «ادامهٔ تماشا» on the home screen opens the latest saved episode directly.
        if (state == null && intent.getBooleanExtra(EXTRA_RESUME, false)) {
            progress.latest(library)?.let { (title, selected) -> play(title, selected) }
        }
        val current = catalogue
        updates.execute {
            val latest = store.refresh(current) ?: return@execute
            ui.post {
                if (!isDestroyed && !isFinishing) {
                    catalogue = latest
                    if (player == null) showEpisodesOrLibrary(series?.id, episode?.id)
                }
            }
        }
    }

    private fun showEpisodesOrLibrary(seriesId: String?, episodeId: String? = null) {
        val title = library.find { it.id == seriesId }
        if (title != null) showEpisodes(title, episodeId) else showLibrary(seriesId)
    }

    private data class Choice(
        val label: String,
        val thumbnail: String? = null,
        val status: Int? = null,
        val action: () -> Unit
    )

    private fun showLibrary(focusId: String? = null) {
        series = null
        episode = null
        val titles = library
        val latest = titles.mapNotNull { title ->
            continuation(title)?.let { (episode, position) -> Triple(title, episode, position) }
        }.maxByOrNull { it.third.updated }
        val resume = latest?.let { (title, episode) ->
            Choice(getString(R.string.series_continue_title, title.title, episodeLabel(title, episode))) {
                play(title, episode)
            }
        }
        val choices = listOfNotNull(resume) + titles.map { title ->
            Choice(title.title, title.thumbnail, statusLabel(title)) { showEpisodes(title) }
        }
        val focus = titles.indexOfFirst { it.id == focusId }.let { if (it < 0) 0 else it + if (resume == null) 0 else 1 }
        showList(getString(R.string.iranian_series), choices, focus)
    }

    private fun continuation(title: IranianSeries): Pair<Episode, EpisodePosition>? = progress.resumable(title)

    private fun showEpisodes(title: IranianSeries, focusId: String? = null) {
        series = title
        episode = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val cards = episodeCards(title)
        val content = column(dp(28))
        content.addView(label(title.title, 32).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL },
            LinearLayout.LayoutParams(-1, dp(50)))
        val summary = getString(R.string.series_available_count, persianNumber(title.episodes.size)) +
            (statusLabel(title)?.let { " · ${getString(it)}" } ?: "")
        content.addView(label(summary, 19).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setTextColor(AppTheme.current.muted)
        }, LinearLayout.LayoutParams(-1, dp(32)))

        val gridId = View.generateViewId()
        val resume = continuation(title)?.let { (selected) ->
            val saved = progress.position(title, selected)?.takeUnless { it.completed }?.seconds ?: 0.0
            label(getString(R.string.series_resume_position, episodeLabel(title, selected), episodeTime(saved)), 23).apply {
                id = View.generateViewId()
                isFocusable = true
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(dp(18), 0, dp(18), 0)
                background = focusBackground()
                nextFocusDownId = gridId
                setOnClickListener { play(title, selected) }
                contentDescription = text
                content.addView(this, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(10) })
            }
        }
        val grid = GridView(this).apply {
            id = gridId
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            numColumns = ((resources.displayMetrics.widthPixels / resources.displayMetrics.density - 56) / 205)
                .toInt().coerceIn(2, 4)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            horizontalSpacing = dp(10)
            verticalSpacing = dp(10)
            setDrawSelectorOnTop(true)
            selector = outline(Color.TRANSPARENT, ACCENT)
            resume?.let { nextFocusUpId = it.id }
            adapter = object : BaseAdapter() {
                override fun getCount() = cards.size
                override fun getItem(position: Int) = cards[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun areAllItemsEnabled() = false
                override fun isEnabled(position: Int) = cards[position].episode != null
                override fun getView(position: Int, recycled: View?, parent: ViewGroup): View =
                    (recycled as? EpisodeCardView ?: EpisodeCardView()).apply { bind(title, cards[position]) }
            }
            setOnItemClickListener { _, _, position, _ -> cards[position].episode?.let { play(title, it) } }
        }
        content.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        display(content)
        val selected = cards.indexOfFirst { it.episode?.id == focusId && it.episode != null }
            .takeIf { it >= 0 } ?: cards.indexOfFirst { it.episode != null }
        grid.setSelection(selected)
        if (focusId == null && resume != null) resume.requestFocus() else grid.requestFocus()
    }

    private inner class EpisodeCardView : LinearLayout(this@IranianSeriesActivity) {
        private val number = label("", 36).apply {
            setTextColor(ACCENT)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        private val heading = label("", 21).apply { gravity = Gravity.START }
        private val detail = label("", 17).apply {
            gravity = Gravity.START
            setTextColor(AppTheme.current.muted)
        }

        init {
            layoutParams = android.widget.AbsListView.LayoutParams(-1, dp(82))
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = focusBackground()
            setPadding(dp(12), dp(7), dp(12), dp(7))
            addView(number, LayoutParams(dp(64), -1).apply { marginEnd = dp(8) })
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                addView(heading, LayoutParams(-1, -2))
                addView(detail, LayoutParams(-1, -2))
            }, LayoutParams(0, -1, 1f))
        }

        fun bind(title: IranianSeries, card: EpisodeCard) {
            val entry = card.episode
            val saved = entry?.let { progress.position(title, it) }
            isEnabled = entry != null
            number.text = card.number?.let(::persianNumber) ?: "—"
            number.setTextColor(if (entry == null) AppTheme.current.muted else ACCENT)
            heading.text = if (title.multipleSeasons) getString(R.string.series_season, persianNumber(card.season))
                else getString(R.string.series_episode_heading)
            detail.text = when {
                entry == null -> getString(R.string.series_episode_unavailable)
                saved?.completed == true -> getString(R.string.series_watched)
                saved != null && saved.seconds > 0 -> getString(R.string.series_progress, episodeTime(saved.seconds))
                else -> getString(R.string.series_play)
            }
            val name = when {
                entry != null -> episodeLabel(title, entry)
                card.number != null -> if (title.multipleSeasons)
                    getString(R.string.series_season_episode, persianNumber(card.season), persianNumber(card.number))
                    else getString(R.string.series_episode, persianNumber(card.number))
                else -> getString(R.string.series_season, persianNumber(card.season))
            }
            contentDescription = "$name · ${detail.text}"
        }
    }

    private fun focusBackground() = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), outline(AppTheme.current.selected, ACCENT))
        addState(intArrayOf(android.R.attr.state_selected), outline(AppTheme.current.selected, ACCENT))
        addState(intArrayOf(), outline(TILE))
    }

    private fun statusLabel(title: IranianSeries): Int? = when (title.availability) {
        SeriesAvailability.COMPLETE -> null
        SeriesAvailability.PARTIAL -> R.string.series_partial
        SeriesAvailability.ONGOING -> R.string.series_ongoing
    }

    private fun showList(title: String, choices: List<Choice>, selected: Int, status: Int? = null) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val content = column(dp(32))
        content.addView(label(title, 32), LinearLayout.LayoutParams(-1, dp(59)))
        status?.let { content.addView(label(getString(it), 20), LinearLayout.LayoutParams(-1, dp(36))) }
        if (choices.isEmpty()) {
            content.addView(label(getString(R.string.series_empty), 24), LinearLayout.LayoutParams(-1, -1))
        } else {
            val list = ListView(this).apply {
                choiceMode = ListView.CHOICE_MODE_NONE
                dividerHeight = dp(6)
                setDrawSelectorOnTop(true)
                selector = outline(Color.TRANSPARENT, ACCENT)
                adapter = object : ArrayAdapter<Choice>(this@IranianSeriesActivity, 0, choices) {
                    override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
                        val row = recycled as? ChoiceRow ?: ChoiceRow()
                        getItem(position)?.let(row::bind)
                        return row
                    }
                }
                setOnItemClickListener { _, _, position, _ -> choices[position].action() }
            }
            content.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
            display(content)
            list.requestFocus()
            list.setSelection(selected)
            return
        }
        display(content)
    }

    private inner class ChoiceRow : LinearLayout(this@IranianSeriesActivity) {
        private val thumbnail = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        private val title = label("", 25)
        private val status = label("", 19)

        init {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(54)
            background = outline(TILE)
            setPadding(dp(12), dp(5), dp(12), dp(5))
            addView(thumbnail, LayoutParams(dp(104), dp(58)).apply { marginEnd = dp(14) })
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                addView(title, LayoutParams(-1, -2))
                addView(status, LayoutParams(-1, -2))
            }, LayoutParams(0, -2, 1f))
        }

        fun bind(choice: Choice) {
            title.text = choice.label
            title.gravity = if (choice.thumbnail == null) Gravity.CENTER else Gravity.START
            status.gravity = title.gravity
            status.visibility = if (choice.status == null) View.GONE else View.VISIBLE
            choice.status?.let(status::setText)
            thumbnail.visibility = if (choice.thumbnail == null) View.GONE else View.VISIBLE
            thumbnails.load(choice.thumbnail, thumbnail)
        }
    }

    private fun play(title: IranianSeries, selected: Episode, restart: Boolean = false, captions: Boolean = false) {
        closePlayer()
        if (restart) progress.clear(title, selected)
        series = title
        episode = selected
        lastSample = null
        lastSaved = null
        finished = false
        unavailable = false
        savedAt = SystemClock.elapsedRealtime()
        resumeSeconds = progress.position(title, selected)?.takeUnless { it.completed }?.seconds ?: 0.0
        reachedStart = resumeSeconds == 0.0
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val content = column(dp(14))
        content.addView(label("${title.title} · ${episodeLabel(title, selected)}", 23),
            LinearLayout.LayoutParams(-1, dp(39)))
        val status = label("", 18).apply { visibility = View.GONE }
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val pause = controls.action(R.string.series_play_pause) { player?.togglePause() }
        val restartButton = controls.action(R.string.series_restart) { play(title, selected, restart = true) }
        val episodesButton = controls.action(R.string.series_episode_list) { returnToEpisodes() }
        val captionsLabel = if (captions) R.string.series_captions_on else R.string.series_captions_off
        val captionsButton = controls.action(captionsLabel) {
            leaveEpisode { play(title, selected, captions = !captions) }
        }
        val nextLabel = if (title.availability == SeriesAvailability.PARTIAL) R.string.series_next_available
            else R.string.series_next_episode
        nextButton = controls.action(nextLabel) {
            title.after(selected)?.let { play(title, it) }
        }.apply { visibility = View.GONE }
        val outside = controls.action(R.string.series_open_youtube) {
            if (openYouTube(selected.watchUrl)) {
                showEpisodesOrLibrary(title.id, selected.id)
            } else {
                status.setText(R.string.series_youtube_missing)
            }
        }.apply { visibility = View.GONE }
        val embed = YouTubePlayer(this, { message ->
            status.visibility = if (message == null) View.GONE else View.VISIBLE
            if (message != null) status.setText(message)
            val blocked = message == R.string.series_embed_blocked
            unavailable = blocked || message == R.string.programme_unavailable
            outside.visibility = if (blocked) View.VISIBLE else View.GONE
            pause.visibility = if (unavailable) View.GONE else View.VISIBLE
            captionsButton.visibility = if (unavailable) View.GONE else View.VISIBLE
            restartButton.visibility = if (blocked) View.GONE else View.VISIBLE
            if (blocked) outside.requestFocus()
            if (unavailable && !blocked) episodesButton.requestFocus()
        }, ::openYouTube)
        player = embed
        content.addView(embed.view, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(status, LinearLayout.LayoutParams(-1, -2))
        content.addView(controls, LinearLayout.LayoutParams(-1, dp(56)))
        display(content)
        pause.requestFocus()
        val subtitles = SubtitlePolicy.forEpisode(captions)
        embed.playEpisode(selected, resumeSeconds, captions = subtitles != SubtitleMode.OFF)
        if (BuildConfig.DEBUG) Log.i("AminSeries", "Open ${title.id}/${selected.id} at $resumeSeconds")
        ui.post(sampleTick)
    }

    private fun recordSample(value: EpisodePlayback, leaving: Boolean = false) {
        val selected = episode ?: return
        val hasPosition = value.state in setOf(ENDED, PLAYING, PAUSED) ||
            (leaving && value.state == BUFFERING && lastSample != null)
        if (value.videoId != selected.videoId || !hasPosition ||
            !value.seconds.isFinite() || !value.duration.isFinite() ||
            value.duration <= 0 || value.seconds <= 0 || value.seconds > value.duration + 1) return
        if (!reachedStart && value.seconds < resumeSeconds - SEEK_TOLERANCE) return
        reachedStart = true
        val ended = value.state == ENDED && value.seconds >= value.duration - SEEK_TOLERANCE
        lastSample = value
        if (BuildConfig.DEBUG) Log.i("AminSeries", "Position ${selected.id} ${value.seconds} state=${value.state}")
        if (ended || value.state != lastSaved?.state || SystemClock.elapsedRealtime() - savedAt >= SAVE_INTERVAL) {
            saveProgress(ended)
        }
        if (ended) {
            finished = true
            if (series?.after(selected) != null) {
                nextButton.visibility = View.VISIBLE
                nextButton.requestFocus()
            }
        }
    }

    private fun saveProgress(completed: Boolean = finished) {
        val title = series ?: return
        val selected = episode ?: return
        val sample = lastSample ?: return
        if (sample == lastSaved) return
        progress.save(title, selected, sample.seconds, completed)
        lastSaved = sample
        savedAt = SystemClock.elapsedRealtime()
    }

    private fun closePlayer() {
        ui.removeCallbacks(sampleTick)
        leavingEpisode = false
        saveProgress()
        player?.let {
            (it.view.parent as? ViewGroup)?.removeView(it.view)
            it.close()
        }
        player = null
    }

    private fun returnToEpisodes() {
        val titleId = series?.id
        val episodeId = episode?.id
        leaveEpisode { showEpisodesOrLibrary(titleId, episodeId) }
    }

    private fun leaveEpisode(after: () -> Unit) {
        val current = player ?: return
        if (leavingEpisode) return
        leavingEpisode = true
        val finish = Runnable {
            if (player === current && leavingEpisode) {
                closePlayer()
                after()
            }
        }
        if (unavailable || finished) {
            finish.run()
            return
        }
        ui.postDelayed(finish, 350)
        current.sampleEpisode { sample ->
            ui.removeCallbacks(finish)
            if (player === current && leavingEpisode) {
                if (sample != null) recordSample(sample, leaving = true)
                finish.run()
            }
        }
    }

    private fun openYouTube(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.google.android.youtube.tv")
        return try {
            startActivity(intent)
            closePlayer()
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    @Deprecated("Back button on Android TV")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val title = series
        val selected = episode
        when {
            selected != null -> returnToEpisodes()
            title != null -> showLibrary(title.id)
            else -> super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        active = true
        player?.view?.onResume()
        ui.post(sampleTick)
    }

    override fun onPause() {
        active = false
        ui.removeCallbacks(sampleTick)
        saveProgress()
        player?.pauseEpisode()
        player?.view?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        closePlayer()
        ui.removeCallbacksAndMessages(null)
        updates.shutdownNow()
        thumbnails.close()
        super.onDestroy()
    }

    private fun episodeLabel(title: IranianSeries, episode: Episode): String =
        if (title.multipleSeasons) getString(R.string.series_season_episode, persianNumber(episode.season), persianNumber(episode.number))
        else getString(R.string.series_episode, persianNumber(episode.number))

    private fun LinearLayout.action(text: Int, run: () -> Unit) = label(getString(text), 21).also {
        it.background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), outline(TILE, ACCENT))
            addState(intArrayOf(), outline(TILE))
        }
        it.isFocusable = true
        it.setOnClickListener { run() }
        addView(it, LinearLayout.LayoutParams(0, dp(49), 1f).apply { setMargins(dp(5), 0, dp(5), 0) })
    }

    private fun column(padding: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setBackgroundColor(BACKGROUND)
        setPadding(padding, padding, padding, padding)
    }

    private fun label(text: String, size: Int) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(AppTheme.current.ink)
    }

    private fun outline(fill: Int, border: Int = fill) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(12).toFloat()
        setStroke(dp(3), border)
    }

    @Suppress("DEPRECATION")
    private fun display(content: View) {
        setContentView(content)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        val BACKGROUND get() = AppTheme.current.background
        val TILE get() = AppTheme.current.surface
        val ACCENT get() = AppTheme.current.accent
        const val SAMPLE_INTERVAL = 1000L
        const val SAVE_INTERVAL = 5000L
        const val SEEK_TOLERANCE = 6.0
        const val ENDED = 0
        const val PLAYING = 1
        const val PAUSED = 2
        const val BUFFERING = 3
    }
}
