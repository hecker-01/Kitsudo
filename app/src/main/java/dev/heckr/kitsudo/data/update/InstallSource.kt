package dev.heckr.kitsudo.data.update

import android.content.Context
import android.os.Build

/**
 * Determines how the app was installed so update handling can branch:
 * Play Store builds defer to Google Play; sideloaded builds use the in-app
 * self-updater that pulls APKs from GitHub releases.
 */
object InstallSource {

    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    /** True when the app was installed by the Google Play Store. */
    fun isFromPlayStore(context: Context): Boolean {
        val installer = try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(context.packageName)
            }
        } catch (_: Exception) {
            null
        }
        return installer == PLAY_STORE_PACKAGE
    }
}
