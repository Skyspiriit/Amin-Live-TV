package com.enigma.aminlivetv

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

internal object ReleaseCheck {
    private const val REPOSITORY = "Skyspiriit/Amin-Live-TV"
    private const val ENDPOINT = "https://api.github.com/repos/$REPOSITORY/releases?per_page=20"
    private const val MAX_BYTES = 512 * 1024
    private val versionPattern = Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")

    fun latest(): Result<String?> = runCatching {
        val endpoint = if (BuildConfig.TEST_CATALOG) URI(BuildConfig.CATALOG_URL).resolve("releases.json")
            else URI(ENDPOINT)
        val connection = endpoint.toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Amin-Live-TV/${BuildConfig.VERSION_NAME}")
            check(connection.responseCode == HttpURLConnection.HTTP_OK)
            val body = connection.inputStream.use { it.readLimited(MAX_BYTES) }
            val releases = JSONArray(body.toString(Charsets.UTF_8))
            val eligibleTags = (0 until releases.length()).asSequence()
                .mapNotNull(releases::optJSONObject)
                .filter { !it.optBoolean("draft", true) }
                .filter(::hasApk)
                .map { it.optString("tag_name") }
            newerTag(BuildConfig.VERSION_NAME, eligibleTags)
        } finally {
            connection.disconnect()
        }
    }

    fun page(tag: String): String = "https://github.com/$REPOSITORY/releases/tag/$tag"

    private fun hasApk(release: JSONObject): Boolean {
        val tag = release.optString("tag_name")
        val name = "Amin-Live-TV-$tag.apk"
        val url = "https://github.com/$REPOSITORY/releases/download/$tag/$name"
        val assets = release.optJSONArray("assets") ?: return false
        return (0 until assets.length()).asSequence().mapNotNull(assets::optJSONObject).any {
            it.optString("name") == name && it.optString("state") == "uploaded" &&
                it.optString("browser_download_url") == url
        }
    }

    internal fun newerTag(installed: String, tags: Sequence<String>): String? {
        val current = parse(installed) ?: return null
        return tags.filter { it.startsWith("v") }
            .mapNotNull { tag -> parse(tag.drop(1))?.let { tag to it } }
            .filter { (_, version) -> version > current }
            .maxByOrNull { (_, version) -> version }?.first
    }

    private fun parse(value: String): Version? {
        val parts = versionPattern.matchEntire(value)?.groupValues ?: return null
        val major = parts[1].toIntOrNull() ?: return null
        val minor = parts[2].toIntOrNull() ?: return null
        val patch = parts[3].toIntOrNull() ?: return null
        return Version(major, minor, patch)
    }

    private data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
        override fun compareTo(other: Version): Int = compareValuesBy(this, other, Version::major, Version::minor, Version::patch)
    }
}
