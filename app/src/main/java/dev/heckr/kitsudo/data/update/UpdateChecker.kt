package dev.heckr.kitsudo.data.update

import android.content.Context
import dev.heckr.kitsudo.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Singleton that checks GitHub for a newer release.
 * Safe to call from Application.onCreate without a ViewModel.
 */
object UpdateChecker {

    var latestVersion: String? = null
        private set
    var latestApkUrl: String? = null
        private set
    var releaseBody: String? = null
        private set
    var apkSizeBytes: Long = 0L
        private set
    var updateAvailable: Boolean = false
        private set
    var lastCheckError: String? = null
        private set

    private val listeners = mutableListOf<() -> Unit>()
    private var isChecking = false

    private const val RELEASES_URL =
        "https://api.github.com/repos/hecker-01/kitsudo/releases/latest"

    private const val RELEASE_BY_TAG_URL =
        "https://api.github.com/repos/hecker-01/kitsudo/releases/tags/"

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun check(context: Context) {
        // Play Store builds update through Google Play; never self-update.
        if (BuildConfig.PLAY_STORE_BUILD || InstallSource.isFromPlayStore(context)) return
        if (isChecking || updateAvailable) return
        isChecking = true

        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        } catch (_: Exception) {
            "0"
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conn = URL(RELEASES_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    lastCheckError = null
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    val tagVersion = json.getString("tag_name").removePrefix("v")

                    if (isNewerVersion(current = currentVersion, latest = tagVersion)) {
                        val assets = json.getJSONArray("assets")
                        var apkUrl: String? = null
                        var apkSize = 0L
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").endsWith(".apk")) {
                                apkUrl = asset.getString("browser_download_url")
                                apkSize = asset.optLong("size", 0L)
                                break
                            }
                        }
                        if (apkUrl != null) {
                            latestApkUrl = apkUrl
                            latestVersion = tagVersion
                            releaseBody = json.optString("body", "")
                            apkSizeBytes = apkSize
                            updateAvailable = true
                        }
                    }
                } else {
                    lastCheckError = "HTTP ${conn.responseCode}"
                }
                conn.disconnect()
            } catch (e: Exception) {
                lastCheckError = e.localizedMessage ?: "Unknown error"
            }

            isChecking = false
            withContext(Dispatchers.Main) { listeners.forEach { it() } }
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

    fun clear() {
        updateAvailable = false
        latestVersion = null
        latestApkUrl = null
        releaseBody = null
        apkSizeBytes = 0L
        lastCheckError = null
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
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
