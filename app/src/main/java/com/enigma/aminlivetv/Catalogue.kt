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

// The four main categories come from `channels`. The others are documentary groups that only
// `programmes` use, so older apps, which reject unknown channel categories, keep updating.
internal enum class Category(val key: String, val title: Int, val main: Boolean = true) {
    DOMESTIC("domestic", R.string.domestic),
    NEWS("news", R.string.news),
    SPORTS("sports", R.string.sports),
    FILM("film", R.string.film),
    DOCUMENTARY("documentary", R.string.documentary, false),
    WILDLIFE("wildlife", R.string.wildlife, false),
    NATURE("nature", R.string.nature, false),
    SCIENCE("science", R.string.science, false),
    MOVIES("movies", R.string.movies, false),
    SERIES("series", R.string.series, false)
}

// Official YouTube playback: the channel's current live broadcast, its playlist, or the live
// broadcast with the playlist as the fallback when nothing is live.
internal data class YouTube(val channel: String?, val playlist: String?)

internal data class Stream(
    val url: String,
    val format: String,
    val priority: Int
) {
    val key: String = "$format:$url"
}

internal enum class Origin(val key: String) {
    DOMESTIC("domestic"),
    SATELLITE("satellite")
}

internal data class Channel(
    val id: String,
    val name: String,
    val categories: Set<Category>,
    val logo: String?,
    val streams: List<Stream>,
    val origin: Origin,
    val status: String,
    val verifiedAt: Long,
    val filmOrder: Int? = null,
    val officialLiveUrl: String? = null,
    val youtube: YouTube? = null
) {
    val sourceSet = streams.map { it.key }.sorted().joinToString("\n")
    val listedInApp = categories.any(::belongsTo)

    fun belongsTo(category: Category): Boolean = category in categories

    companion object {
        const val BBC_PERSIAN = "bbc-persian"
    }
}

internal data class Catalogue(
    val revision: Int,
    val channels: List<Channel>,
    val iranianSeries: List<IranianSeries> = emptyList()
) {
    fun inCategory(category: Category): List<Channel> {
        val matching = channels.filter { it.belongsTo(category) }
        return if (category == Category.FILM && matching.any { it.filmOrder != null }) {
            matching.sortedBy { it.filmOrder ?: Int.MAX_VALUE }
        } else {
            matching.sortedBy { it.streams.isEmpty() }
        }
    }
}

internal class CatalogueStore(context: Context) {
    private val assets = context.assets
    private val cache = AtomicFile(File(context.filesDir, "channels.json"))

    fun load(): Catalogue {
        val bundled = assets.open("channels.json").use { parse(it.readLimited(MAX_BYTES)) }
        val cached = synchronized(cacheLock) { readCache() }
        return cached?.takeIf { it.canReplace(bundled) } ?: bundled
    }

    private fun readCache(): Catalogue? =
        runCatching { cache.openRead().use { parse(it.readLimited(MAX_BYTES)) } }.getOrNull()

    fun refresh(current: Catalogue): Catalogue? = runCatching {
        val connection = URI(BuildConfig.CATALOG_URL).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 7000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json, text/plain")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val bytes = connection.inputStream.use { it.readLimited(MAX_BYTES) }
            val document = readDocument(bytes)
            if ((document.get("revision") as Int) <= current.revision) return null
            val candidate = parse(document)
            if (!candidate.canReplace(current)) return null

            synchronized(cacheLock) {
                // Both catalogue screens can refresh. A slower response must never overwrite a newer cache.
                val cached = readCache()?.takeIf { it.canReplace(current) }
                if (cached != null && !candidate.canReplace(cached)) return cached
                val output = cache.startWrite()
                try {
                    output.write(bytes)
                    cache.finishWrite(output)
                } catch (failure: Exception) {
                    cache.failWrite(output)
                    throw failure
                }
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

    private fun parse(bytes: ByteArray): Catalogue = parse(readDocument(bytes))

    private fun readDocument(bytes: ByteArray): JSONObject {
        val tokenizer = JSONTokener(bytes.toString(Charsets.UTF_8))
        val json = tokenizer.nextValue() as JSONObject
        require(tokenizer.nextClean() == '\u0000')
        return json
    }

    private fun parse(json: JSONObject): Catalogue {
        require(json.get("schemaVersion") == 1)
        val revision = json.get("revision") as Int
        require(revision > 0)
        val entries = json.getJSONArray("channels")
        require(entries.length() in 1..2000)
        val ids = HashSet<String>()
        val filmPositions = HashSet<Int>()
        val channels = (0 until entries.length()).map { index ->
            val entry = entries.getJSONObject(index)
            val id = entry.requiredText("id", 80)
            require(ID.matches(id) && ids.add(id))
            val name = entry.requiredText("name", 100)
            if (!entry.isNull("englishName")) entry.requiredText("englishName", 100)
            val status = entry.requiredText("status", 16)
            require(status in setOf("verified", "unverified", "unavailable", "restricted"))
            val date = entry.get("lastVerified")
            val verifiedAt = if (date == JSONObject.NULL) 0L else LocalDate.parse(date as String).toEpochDay() * DAY_MS
            if (status == "verified" || status == "unavailable") require(date != JSONObject.NULL)
            val origin = Origin.entries.first { it.key == entry.requiredText("origin", 12) }
            val logo = entry.get("logo").let { if (it == JSONObject.NULL) null else publicUrl(it as String) }
            val attributions = entry.getJSONArray("sourceAttribution").strings()
            require(attributions.isNotEmpty())
            attributions.forEach { publicUrl(it) }
            val keys = entry.getJSONArray("categories").strings()
            require(keys.isNotEmpty() && keys.distinct().size == keys.size)
            val categories = keys.mapTo(LinkedHashSet()) { key ->
                Category.entries.first { it.main && it.key == key }
            }
            val filmOrder = if (entry.isNull("filmOrder")) null else entry.get("filmOrder") as Int
            if (Category.FILM in categories) {
                if (revision >= 3) require(filmOrder != null)
                if (filmOrder != null) require(filmOrder >= 0 && filmPositions.add(filmOrder))
            } else {
                require(filmOrder == null)
            }
            val officialLiveUrl = if (entry.isNull("officialLiveUrl")) null else
                publicUrl(entry.requiredText("officialLiveUrl", 2048)).also { url ->
                    require(Category.NEWS in categories)
                    require(URI(url).host?.lowercase() in OFFICIAL_LIVE_HOSTS)
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
            val youtube = entry.youtube()?.also { require(Category.NEWS in categories && it.playlist == null) }
            Channel(id, name, categories, logo, streams, origin, status, verifiedAt, filmOrder, officialLiveUrl, youtube)
        }
        require(Category.entries.filter { it.main }.all { category -> channels.any { it.belongsTo(category) } })
        require(channels.any { it.id == Channel.BBC_PERSIAN && it.belongsTo(Category.NEWS) })
        val programmes = parseProgrammes(json.optJSONArray("programmes"), channels, ids) +
            parseProgrammes(json.optJSONArray("programmes2"), channels, ids)
        return Catalogue(revision, channels + programmes, parseIranianSeries(json))
    }

    // Documentary, movie and series groups. Each programme plays a catalogue channel natively
    // (`channelRef`) or official YouTube content, and belongs to exactly one group. `programmes`
    // holds the groups 0.3.0 and 0.3.1 know; `programmes2` holds later ones, which those versions
    // ignore. A group this version doesn't know is skipped rather than rejecting the catalogue.
    private fun parseProgrammes(entries: JSONArray?, channels: List<Channel>, ids: MutableSet<String>): List<Channel> {
        if (entries == null) return emptyList()
        require(entries.length() <= 300)
        return (0 until entries.length()).mapNotNull { index ->
            val entry = entries.getJSONObject(index)
            val id = entry.requiredText("id", 80)
            require(ID.matches(id) && ids.add(id))
            val name = entry.requiredText("name", 100)
            val groupKey = entry.requiredText("group", 16)
            val group = Category.entries.firstOrNull { !it.main && it.key == groupKey } ?: return@mapNotNull null
            entry.getJSONArray("sourceAttribution").strings().also { require(it.isNotEmpty()) }.forEach { publicUrl(it) }
            val reference = if (entry.isNull("channelRef")) null else entry.requiredText("channelRef", 80)
            val youtube = entry.youtube()
            require((reference == null) != (youtube == null))
            if (reference != null) {
                val base = channels.first { it.id == reference }
                base.copy(id = id, name = name, categories = setOf(group), filmOrder = null, officialLiveUrl = null)
            } else {
                Channel(id, name, setOf(group), null, emptyList(), Origin.SATELLITE, "unverified", 0L, youtube = youtube)
            }
        }
    }

    private fun JSONObject.youtube(): YouTube? {
        val channel = if (isNull("youtubeChannel")) null else requiredText("youtubeChannel", 24)
        val playlist = if (isNull("youtubePlaylist")) null else requiredText("youtubePlaylist", 64)
        if (channel == null && playlist == null) return null
        channel?.let { require(YOUTUBE_CHANNEL.matches(it)) }
        playlist?.let { require(YOUTUBE_PLAYLIST.matches(it)) }
        return YouTube(channel, playlist)
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
        val cacheLock = Any()
        val ID = Regex("[a-z0-9][a-z0-9_-]*")
        val YOUTUBE_CHANNEL = Regex("UC[A-Za-z0-9_-]{22}")
        val YOUTUBE_PLAYLIST = Regex("(PL|UU)[A-Za-z0-9_-]{10,62}")
        val OFFICIAL_LIVE_HOSTS = setOf(
            "www.bbc.com", "www.dw.com", "www.france24.com", "www.euronews.com",
            "www.aljazeera.com", "news.sky.com", "abcnews.go.com", "www.cbsnews.com", "www.youtube.com"
        )
        const val MAX_BYTES = 2 * 1024 * 1024
        const val DAY_MS = 86_400_000L
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
