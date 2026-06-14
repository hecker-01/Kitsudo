package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.data.backup.BackupFile
import dev.heckr.kitsudo.data.backup.BackupNotificationPrefs
import dev.heckr.kitsudo.data.backup.BackupSettings
import dev.heckr.kitsudo.data.sync.TaskDto
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import dev.heckr.kitsudo.domain.repository.TaskListPreferencesRepository
import dev.heckr.kitsudo.domain.repository.TaskRepository
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Serializes all tasks **and** app settings into a pretty-printed [BackupFile] JSON. */
class ExportTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val themeRepository: ThemeRepository,
    private val notificationPreferences: NotificationPreferencesRepository,
    private val taskListPreferences: TaskListPreferencesRepository,
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(): String {
        val tasks = repository.getAllTasks().map(TaskDto::fromDomain)
        val notif = notificationPreferences.observe().first()
        val settings = BackupSettings(
            themePalette = themeRepository.getThemePalette().first().name,
            accent = themeRepository.getAccent().first().name,
            sortMode = taskListPreferences.observeSortMode().first().name,
            notifications = BackupNotificationPrefs(
                preReminderLeadMinutes = notif.preReminderLeadMinutes.toList(),
                quietHoursEnabled = notif.quietHoursEnabled,
                quietStartMinutes = notif.quietStartMinutes,
                quietEndMinutes = notif.quietEndMinutes,
                snoozeMinutes = notif.snoozeMinutes,
            ),
        )
        val backup = BackupFile(exportedAt = System.currentTimeMillis(), tasks = tasks, settings = settings)
        return json.encodeToString(backup)
    }
}
