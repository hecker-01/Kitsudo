package dev.heckr.kitsudo.presentation.settings

import dev.heckr.kitsudo.data.update.AppUpdater
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.NotificationPreferences
import dev.heckr.kitsudo.domain.model.ThemePalette

data class SettingsUiState(
    val palette: ThemePalette = ThemePalette.MOCHA,
    val accent: CatppuccinAccent = CatppuccinAccent.default,
    val notifications: NotificationPreferences = NotificationPreferences(),
    val updateStatus: AppUpdater.Status = AppUpdater.Status.Idle,
    val showUpdateDialog: Boolean = false,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val isDebug: Boolean = false,
    val packageName: String = "",
)
