package dev.heckr.kitsudo.data.backup

import dev.heckr.kitsudo.data.sync.TaskDto
import kotlinx.serialization.Serializable

/**
 * On-disk backup envelope written/read by the export & import flows. Captures
 * everything user-owned: the full task tree plus app [settings].
 *
 * [schemaVersion] lets future imports detect and migrate older formats; it is
 * bumped only when the shape changes incompatibly. Every field is defaulted so
 * older backups (e.g. without [settings]) still deserialize.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val tasks: List<TaskDto> = emptyList(),
    val settings: BackupSettings? = null,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

/** App preferences captured in a backup. Enums are stored by their `name`. */
@Serializable
data class BackupSettings(
    val themePalette: String? = null,
    val accent: String? = null,
    val sortMode: String? = null,
    val notifications: BackupNotificationPrefs? = null,
)

@Serializable
data class BackupNotificationPrefs(
    val preReminderLeadMinutes: List<Int> = emptyList(),
    val quietHoursEnabled: Boolean = false,
    val quietStartMinutes: Int = 22 * 60,
    val quietEndMinutes: Int = 7 * 60,
    val snoozeMinutes: Int = 10,
)
