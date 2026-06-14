package dev.heckr.kitsudo.data.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import dev.heckr.kitsudo.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdater @Inject constructor() {

    sealed class Status {
        data object Idle : Status()
        data object Checking : Status()
        data class Available(val version: String) : Status()
        /** progress: 0-100 for determinate, -1 for indeterminate */
        data class Downloading(val progress: Int = -1) : Status()
        data object Installing : Status()
        data object UpToDate : Status()
        data class Error(val message: String) : Status()
    }

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    /**
     * Emits an Intent that the Composable must launch via ActivityResultLauncher
     * (the MANAGE_UNKNOWN_APP_SOURCES settings screen).
     */
    private val _installPermissionIntent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val installPermissionIntent: SharedFlow<Intent> = _installPermissionIntent.asSharedFlow()

    private var pendingInstallFile: File? = null
    private var downloadJob: Job? = null

    /** App-lifetime scope for the check + download (this is an app-scoped @Singleton). */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The newest release found by the last successful check, if any. */
    private var available: UpdateChecker.UpdateInfo? = null

    /** The pending update's details (version/notes/size), for the confirm dialog. */
    val availableUpdate: UpdateChecker.UpdateInfo?
        get() = available

    /** Guards against firing more than one silent background check per process. */
    private var silentCheckStarted = false

    /**
     * Kicks off a release check and reflects the outcome in [status].
     *
     * [silent] (the app-launch background check) only surfaces an available
     * update - it never shows "up to date"/error unsolicited, and runs at most
     * once per process. A non-silent check (user tapped the update card) shows
     * the full Checking -> UpToDate/Error/Available flow.
     */
    fun checkForUpdates(context: Context, silent: Boolean = true) {
        // Play Store builds update through Google Play; never self-check.
        if (BuildConfig.PLAY_STORE_BUILD || isInstalledFromPlayStore(context)) return
        when (status.value) {
            is Status.Checking, is Status.Downloading, is Status.Installing, is Status.Available -> return
            else -> Unit
        }
        if (silent && silentCheckStarted) return
        if (silent) silentCheckStarted = true else _status.value = Status.Checking

        val currentVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0"

        scope.launch {
            when (val result = UpdateChecker.check(currentVersion)) {
                is UpdateChecker.Result.Available -> {
                    available = result.info
                    _status.value = Status.Available(result.info.version)
                }
                is UpdateChecker.Result.UpToDate ->
                    _status.value = if (silent) Status.Idle else Status.UpToDate
                is UpdateChecker.Result.Error ->
                    _status.value = if (silent) Status.Idle else Status.Error(result.message)
            }
        }
    }

    /** True when the app was installed from the Google Play Store. */
    fun isInstalledFromPlayStore(context: Context): Boolean =
        InstallSource.isFromPlayStore(context)

    /** Opens the app's Google Play listing, falling back to the web URL. */
    fun openPlayStore(context: Context) {
        val pkg = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * Called when the user taps the update card.
     * Returns true if an update is already known (caller should show the confirm dialog).
     */
    fun onUpdateTapped(context: Context): Boolean {
        // Play Store builds hand off to Google Play rather than self-updating.
        if (BuildConfig.PLAY_STORE_BUILD || isInstalledFromPlayStore(context)) {
            openPlayStore(context)
            return false
        }
        return when (_status.value) {
            is Status.Available -> true
            is Status.Downloading, is Status.Installing -> false
            else -> {
                checkForUpdates(context, silent = false)
                false
            }
        }
    }

    /** Call after the user confirms the update dialog. */
    fun startDownload(context: Context) {
        val info = available
        if (info == null) {
            _status.value = Status.Error("Update info missing")
            return
        }
        _status.value = Status.Downloading(-1)
        downloadApk(context, info.apkUrl, info.version)
    }

    /** Call from the ActivityResultCallback for MANAGE_UNKNOWN_APP_SOURCES. */
    fun onInstallPermissionResult(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallFile?.let { installApk(context, it) }
        } else {
            Toast.makeText(context, "Install permission denied", Toast.LENGTH_SHORT).show()
            _status.value = Status.Error("Install permission denied")
        }
        pendingInstallFile = null
    }

    fun cancel() {
        downloadJob?.cancel()
        if (_status.value is Status.Downloading) {
            _status.value = Status.Available(available?.version ?: "")
        }
    }

    // -- Download --------------------------------------------------------------

    private fun downloadApk(context: Context, apkUrl: String, version: String) {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        val outFile = File(dir, "kitsudo-$version.apk")

        // Skip download if file already exists with matching size
        val expectedSize = available?.apkSizeBytes ?: 0L
        if (outFile.exists() && expectedSize > 0L && outFile.length() == expectedSize) {
            _status.value = Status.Installing
            installApk(context, outFile)
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch {
            var conn: HttpURLConnection? = null
            var success = false
            try {
                conn = URL(apkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                conn.requestMethod = "GET"
                conn.connect()

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        _status.value = Status.Error("HTTP ${conn.responseCode}")
                    }
                    return@launch
                }

                val totalBytes = conn.contentLengthLong
                var bytesRead = 0L
                var lastPercent = -1

                conn.inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var n: Int
                        while (input.read(buffer).also { n = it } != -1) {
                            output.write(buffer, 0, n)
                            bytesRead += n
                            if (totalBytes > 0) {
                                val pct = ((bytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastPercent) {
                                    lastPercent = pct
                                    withContext(Dispatchers.Main) {
                                        _status.value = Status.Downloading(pct)
                                    }
                                }
                            }
                        }
                    }
                }

                success = true
                withContext(Dispatchers.Main) {
                    _status.value = Status.Installing
                    installApk(context, outFile)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _status.value = Status.Error(e.message ?: "Download failed")
                }
            } finally {
                conn?.disconnect()
                if (!success) outFile.delete()
            }
        }
    }

    // -- Install ---------------------------------------------------------------

    private fun installApk(context: Context, file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                pendingInstallFile = file
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                _installPermissionIntent.tryEmit(intent)
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            _status.value = Status.Error("Install failed: ${e.message}")
        }
    }
}
