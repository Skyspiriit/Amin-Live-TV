package com.enigma.aminlivetv

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate

internal enum class Category(val key: String, val title: Int) {
    DOMESTIC("domestic", R.string.domestic),
    NEWS("news", R.string.news),
    SPORTS("sports", R.string.sports),
    FILM("film", R.string.film)
}

internal data class Stream(
    val url: String,
    val format: String,
    val priority: Int
)

internal data class Channel(
    val id: String,
    val name: String,
    val categories: Set<Category>,
    val logo: String?,
    val streams: List<Stream>
)

internal data class Catalogue(val revision: Int, val channels: List<Channel>) {
    fun inCategory(category: Category): List<Channel> =
        channels.filter { category in it.categories }
}

internal class CatalogueStore(context: Context) {
    private val assets = context.assets
    private val cache = AtomicFile(File(context.filesDir, "channels.json"))

    fun load(): Catalogue {
        val bundled = assets.open("channels.json").use { parse(it.readLimited(MAX_BYTES)) }
        val cached = runCatching { cache.openRead().use { parse(it.readLimited(MAX_BYTES)) } }.getOrNull()
        return cached?.takeIf { it.canReplace(bundled) } ?: bundled
    }

    fun refresh(current: Catalogue): Catalogue? = runCatching {
        val connection = URI(BuildConfig.CATALOG_URL).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 7000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json, text/plain")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val bytes = connection.inputStream.use { it.readLimited(MAX_BYTES) }
            val candidate = parse(bytes)
            if (!candidate.canReplace(current)) return null

            val output = cache.startWrite()
            try {
                output.write(bytes)
                cache.finishWrite(output)
            } catch (failure: Exception) {
                cache.failWrite(output)
                throw failure
            }
            Log.i("AminCatalogue", "Activated revision ${candidate.revision}")
            candidate
        } finally {
            connection.disconnect()
        }
    }.getOrElse {
        Log.w("AminCatalogue", "Catalogue update rejected or unavailable", it)
        null
    }

    private fun Catalogue.canReplace(other: Catalogue): Boolean {
        if (revision <= other.revision) return false
        val ids = channels.mapTo(HashSet()) { it.id }
        return other.channels.all { it.id in ids }
    }

    private fun parse(bytes: ByteArray): Catalogue {
        val tokenizer = JSONTokener(bytes.toString(Charsets.UTF_8))
        val json = tokenizer.nextValue() as JSONObject
        require(tokenizer.nextClean() == '\u0000')
        require(json.get("schemaVersion") == 1)
        val revision = json.get("revision") as Int
        require(revision > 0)
        val entries = json.getJSONArray("channels")
        require(entries.length() in 1..2000)
        val ids = HashSet<String>()
        val channels = (0 until entries.length()).map { index ->
            val entry = entries.getJSONObject(index)
            val id = entry.requiredText("id", 80)
            require(ID.matches(id) && ids.add(id))
            val name = entry.requiredText("name", 100)
            if (!entry.isNull("englishName")) entry.requiredText("englishName", 100)
            val status = entry.requiredText("status", 16)
            require(status in setOf("verified", "unverified", "unavailable", "restricted"))
            val date = entry.get("lastVerified")
            if (date != JSONObject.NULL) LocalDate.parse(date as String)
            if (status == "verified" || status == "unavailable") require(date != JSONObject.NULL)
            val logo = entry.get("logo").let { if (it == JSONObject.NULL) null else publicUrl(it as String) }
            val attributions = entry.getJSONArray("sourceAttribution").strings()
            require(attributions.isNotEmpty())
            attributions.forEach { publicUrl(it) }
            val keys = entry.getJSONArray("categories").strings()
            require(keys.isNotEmpty() && keys.distinct().size == keys.size)
            val categories = keys.mapTo(LinkedHashSet()) { key ->
                Category.entries.first { it.key == key }
            }
            val sources = entry.getJSONArray("streams")
            require(sources.length() <= 6)
            val streams = (0 until sources.length()).map { offset ->
                val source = sources.getJSONObject(offset)
                val format = source.requiredText("type", 12)
                require(format in setOf("hls", "dash", "progressive"))
                val priority = source.get("priority") as Int
                require(priority in 1..999)
                publicUrl(source.requiredText("attribution", 2048))
                val streamDate = source.get("lastVerified")
                if (streamDate != JSONObject.NULL) LocalDate.parse(streamDate as String)
                if (status == "verified") require(streamDate != JSONObject.NULL)
                Stream(publicUrl(source.requiredText("url", 2048)), format, priority)
            }.sortedBy { it.priority }
            require(streams.map { it.priority }.distinct().size == streams.size)
            require(streams.map { it.url }.distinct().size == streams.size)
            if (status == "verified") require(streams.isNotEmpty())
            if (status == "restricted" || status == "unavailable") require(streams.isEmpty())
            Channel(id, name, categories, logo, streams)
        }
        require(Category.entries.all { category -> channels.any { category in it.categories } })
        return Catalogue(revision, channels)
    }

    private fun publicUrl(value: String): String {
        require(value.length <= 2048)
        val uri = URI(value)
        require(uri.host != null && uri.userInfo == null && uri.fragment == null)
        val testLocal = BuildConfig.TEST_CATALOG && uri.scheme == "http" && uri.host == "127.0.0.1"
        require(uri.scheme == "https" || testLocal)
        return value
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map {
        (get(it) as String).also { value -> require(value.isNotBlank()) }
    }

    private fun JSONObject.requiredText(key: String, maxLength: Int): String =
        (get(key) as String).also { require(it.isNotBlank() && it.length <= maxLength) }

    private companion object {
        val ID = Regex("[a-z0-9][a-z0-9_-]*")
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}

internal fun InputStream.readLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val size = read(buffer)
        if (size < 0) break
        require(output.size() + size <= limit)
        output.write(buffer, 0, size)
    }
    return output.toByteArray()
}
