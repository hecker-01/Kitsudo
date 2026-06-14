package dev.heckr.kitsudo.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Stateless GitHub release queries. Holds no mutable state and owns no coroutine
 * scope - callers ([AppUpdater]) own the state and decide when to run a check.
 */
object UpdateChecker {

    /** Details of a newer release found on GitHub. */
    data class UpdateInfo(
        val version: String,
        val apkUrl: String,
        val apkSizeBytes: Long,
        val body: String,
    )

    /** Outcome of a [check]. */
    sealed interface Result {
        data class Available(val info: UpdateInfo) : Result
        data object UpToDate : Result
        data class Error(val message: String) : Result
    }

    private const val RELEASES_URL =
        "https://api.github.com/repos/hecker-01/kitsudo/releases/latest"

    private const val RELEASE_BY_TAG_URL =
        "https://api.github.com/repos/hecker-01/kitsudo/releases/tags/"

    /**
     * Queries the latest GitHub release and compares it against [currentVersion].
     * Runs on [Dispatchers.IO]; never throws (failures map to [Result.Error]).
     */
    suspend fun check(currentVersion: String): Result = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.Error("HTTP ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tagVersion = json.getString("tag_name").removePrefix("v")

            if (!isNewerVersion(current = currentVersion, latest = tagVersion)) {
                return@withContext Result.UpToDate
            }

            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    return@withContext Result.Available(
                        UpdateInfo(
                            version = tagVersion,
                            apkUrl = asset.getString("browser_download_url"),
                            apkSizeBytes = asset.optLong("size", 0L),
                            body = json.optString("body", ""),
                        ),
                    )
                }
            }
            // Newer tag exists but no APK asset attached yet.
            Result.UpToDate
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Unknown error")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Fetches the release-notes markdown for a specific version's tag, used by
     * the "What's New" sheet after an update so the notes match what was just
     * installed. Tags are published without a `v` prefix, so the bare
     * `<version>` tag is tried first, with `v<version>` as a fallback.
     * Returns null on any failure (offline, no matching release, etc.).
     */
    suspend fun fetchReleaseNotes(versionName: String): String? = withContext(Dispatchers.IO) {
        val clean = versionName.substringBefore("-") // drop "-dev" / build suffixes
        fetchBodyForTag(clean) ?: fetchBodyForTag("v$clean")
    }

    private fun fetchBodyForTag(tag: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(RELEASE_BY_TAG_URL + tag).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                JSONObject(conn.inputStream.bufferedReader().readText())
                    .optString("body", "")
                    .ifBlank { null }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** Pure semver-ish comparison; tolerant of `-suffix` and uneven segment counts. */
    internal fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val cur = current.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
            val lat = latest.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(cur.size, lat.size)) {
                val c = cur.getOrElse(i) { 0 }
                val l = lat.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
