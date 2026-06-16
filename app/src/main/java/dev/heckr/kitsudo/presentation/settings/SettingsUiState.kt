package dev.heckr.kitsudo.presentation.settings

import dev.heckr.kitsudo.data.update.AppUpdater
import dev.heckr.kitsudo.data.update.UpdateChecker
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.NotificationPreferences
import dev.heckr.kitsudo.domain.model.ThemePalette

data class SettingsUiState(
    val palette: ThemePalette = ThemePalette.MOCHA,
    val accent: CatppuccinAccent = CatppuccinAccent.default,
    val notifications: NotificationPreferences = NotificationPreferences(),
    val updateStatus: AppUpdater.Status = AppUpdater.Status.Idle,
    val updateInfo: UpdateChecker.UpdateInfo? = null,
    /**
     * Snapshot taken when this settings screen opened: true if an update was
     * already known to be available. Drives moving the Updates section to the
     * top (with a "!" badge). Deliberately *not* updated mid-session, so a manual
     * "check for updates" that finds one only reorders on the next open.
     */
    val pinUpdatesToTop: Boolean = false,
    val usePlayStoreUpdates: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val showImportModeDialog: Boolean = false,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val isDebug: Boolean = false,
    val packageName: String = "",
)
