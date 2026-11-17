package com.enigma.aminlivetv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors

internal class Logos : AutoCloseable {
    private val io = Executors.newFixedThreadPool(3)
    private val images = object : LruCache<String, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val failed = HashSet<String>()
    private val waiting = HashMap<String, MutableList<java.lang.ref.WeakReference<ImageView>>>()
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    fun load(url: String?, view: ImageView) {
        view.tag = url
        view.setImageResource(R.drawable.channel_placeholder)
        if (url == null || url in failed) return
        images.get(url)?.let {
            show(view, it)
            return
        }
        val reference = java.lang.ref.WeakReference(view)
        waiting[url]?.let {
            it.add(reference)
            return
        }
        waiting[url] = mutableListOf(reference)
        io.execute {
            val bitmap = runCatching { download(url) }.getOrNull()
            main.post {
                val targets = waiting.remove(url) ?: return@post
                if (bitmap == null) {
                    failed.add(url)
                } else {
                    images.put(url, bitmap)
                    targets.mapNotNull { it.get() }.filter { it.tag == url }.forEach { show(it, bitmap) }
                }
            }
        }
    }

    private fun show(view: ImageView, image: Bitmap) {
        view.setImageBitmap(image)
    }

    private fun download(url: String): Bitmap? {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.instanceFollowRedirects = false
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val bytes = connection.inputStream.use { it.readLimited(384 * 1024) }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val dimension = maxOf(bounds.outWidth, bounds.outHeight)
            if (dimension !in 1..4096) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = Integer.highestOneBit((dimension / 200).coerceAtLeast(1))
            }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } finally {
            connection.disconnect()
        }
    }

    override fun close() {
        io.shutdownNow()
        waiting.clear()
        main.removeCallbacksAndMessages(null)
        images.evictAll()
    }
}
