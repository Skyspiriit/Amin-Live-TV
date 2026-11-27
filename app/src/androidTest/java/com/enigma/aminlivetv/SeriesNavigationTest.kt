package com.enigma.aminlivetv

import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Uses actual bundled titles and the Android remote-key path. Never targets production data.
// These UI tests do not claim that YouTube media played or that a physical TV was tested.
@RunWith(AndroidJUnit4::class)
class SeriesNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var catalogue: Catalogue
    private var scenario: ActivityScenario<IranianSeriesActivity>? = null

    @Before fun preparePreview() {
        check(context.packageName.endsWith(".qa")) { "UI tests require the isolated preview" }
        context.getSharedPreferences("iranian-series-progress", 0).edit().clear().commit()
        catalogue = CatalogueStore(context).load()
    }

    @After fun closePreview() { scenario?.close() }

    private fun launch() {
        scenario = ActivityScenario.launch(Intent(context, IranianSeriesActivity::class.java))
        instrumentation.waitForIdleSync()
    }

    private fun choose(title: IranianSeries, hasResume: Boolean = false) {
        scenario!!.onActivity { activity ->
            val list = views(activity.window.decorView).filterIsInstance<ListView>().single()
            list.setSelection(catalogue.iranianSeries.indexOfFirst { it.id == title.id } + if (hasResume) 1 else 0)
            list.requestFocus()
        }
        instrumentation.waitForIdleSync()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
    }

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        instrumentation.waitForIdleSync()
    }

    @Test fun gapsKeepTheirNumbersAndRemoteFocusSkipsUnavailableCards() {
        val title = catalogue.iranianSeries.first { entry ->
            entry.episodes.first().season == 1 && entry.episodes.first().number == 2
        }
        launch()
        choose(title)
        scenario!!.onActivity { activity ->
            val grid = views(activity.window.decorView).filterIsInstance<GridView>().single()
            assertEquals(View.LAYOUT_DIRECTION_RTL, grid.layoutDirection)
            assertTrue(grid.numColumns in 2..4)
            assertFalse(grid.adapter.isEnabled(0))
            assertTrue(grid.adapter.isEnabled(1))
            assertEquals(1, grid.selectedItemPosition)
            assertTrue(grid.getChildAt(0).contentDescription.toString().contains("قسمت ۱"))
            assertTrue(grid.getChildAt(0).contentDescription.toString().contains("در دسترس نیست"))
        }
        capture("series-missing-first-episode")
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        scenario!!.onActivity { activity ->
            val grid = views(activity.window.decorView).filterIsInstance<GridView>().single()
            assertEquals(2, grid.selectedItemPosition)
        }
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        scenario!!.onActivity { activity ->
            val grid = views(activity.window.decorView).filterIsInstance<GridView>().single()
            assertEquals(1, grid.selectedItemPosition)
        }
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        scenario!!.onActivity { activity ->
            val grid = views(activity.window.decorView).filterIsInstance<GridView>().single()
            assertTrue(grid.selectedItemPosition >= grid.numColumns)
            assertTrue(grid.adapter.isEnabled(grid.selectedItemPosition))
        }
        key(KeyEvent.KEYCODE_BACK)
        scenario!!.onActivity { activity ->
            val list = views(activity.window.decorView).filterIsInstance<ListView>().single()
            assertEquals(catalogue.iranianSeries.indexOfFirst { it.id == title.id }, list.selectedItemPosition)
        }
    }

    @Test fun resumeIsOneClickAwayAndPersistsAfterActivityRestart() {
        val title = catalogue.iranianSeries.first()
        val episode = title.episodes.first()
        SeriesProgress(context).save(title, episode, 137.25, false)
        repeat(2) {
            launch()
            choose(title, hasResume = true)
            scenario!!.onActivity { activity ->
                val focused = activity.currentFocus as TextView
                assertTrue(focused.text.toString().startsWith("ادامهٔ تماشا"))
                assertTrue(focused.text.toString().contains("۲:۱۷"))
                val grid = views(activity.window.decorView).filterIsInstance<GridView>().single()
                assertTrue(grid.getChildAt(0).contentDescription.toString().contains("ادامه از ۲:۱۷"))
                assertEquals(137.25, SeriesProgress(activity).position(title, episode)!!.seconds, 0.001)
            }
            capture("series-resume-cards")
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            scenario!!.onActivity { activity -> assertTrue(activity.currentFocus is GridView) }
            key(KeyEvent.KEYCODE_DPAD_UP)
            scenario!!.onActivity { activity -> assertTrue(activity.currentFocus is TextView) }
            scenario!!.close()
            scenario = null
        }
        val saved = SeriesProgress(context).position(title, episode)!!
        assertEquals(episode.videoId, saved.videoId)
        assertEquals(episode.id, saved.episodeId)
        assertEquals(137.25, saved.seconds, 0.001)
        assertEquals(SubtitleMode.OFF, SubtitlePolicy.forEpisode(false))
    }

    private fun capture(name: String) {
        val drawn = CountDownLatch(1)
        scenario!!.onActivity { activity ->
            activity.window.decorView.postOnAnimation {
                activity.window.decorView.postOnAnimation { drawn.countDown() }
            }
        }
        assertTrue("Episode screen rendered", drawn.await(3, TimeUnit.SECONDS))
        instrumentation.uiAutomation.waitForIdle(200, 3000)
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "screenshots").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
}
