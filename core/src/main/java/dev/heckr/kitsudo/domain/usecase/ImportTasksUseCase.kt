package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.data.backup.BackupFile
import dev.heckr.kitsudo.data.backup.BackupSettings
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.Tag
import dev.heckr.kitsudo.domain.model.TaskSortMode
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import dev.heckr.kitsudo.domain.repository.TagRepository
import dev.heckr.kitsudo.domain.repository.TaskListPreferencesRepository
import dev.heckr.kitsudo.domain.repository.TaskRepository
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Restores tasks and settings from a [BackupFile] JSON string, merging or replacing. */
class ImportTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val tagRepository: TagRepository,
    private val themeRepository: ThemeRepository,
    private val notificationPreferences: NotificationPreferencesRepository,
    private val taskListPreferences: TaskListPreferencesRepository,
) {
    enum class Mode {
        /** Add imported tasks alongside existing ones (same id is updated). */
        MERGE,

        /** Wipe the current tasks and restore the backup exactly. */
        REPLACE,
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Selectively restores from a backup. The user chooses what to bring in:
     *
     * @param importTasks    when true, restores the task list using [mode].
     * @param importSettings when true, restores app settings (theme/accent/sort/notifications).
     * @param mode           merge vs replace for tasks; ignored when [importTasks] is false.
     *
     * Returns the number of tasks imported (0 if tasks weren't selected), or a
     * failure for malformed input.
     */
    suspend operator fun invoke(
        rawJson: String,
        importTasks: Boolean,
        importSettings: Boolean,
        mode: Mode,
    ): Result<Int> = runCatching {
        val backup = json.decodeFromString<BackupFile>(rawJson)

        var importedCount = 0
        if (importTasks) {
            val tasks = backup.tasks.map { it.toDomain() }
            when (mode) {
                Mode.MERGE -> repository.insertTasks(tasks).getOrThrow()
                Mode.REPLACE -> {
                    repository.replaceAllTasks(tasks).getOrThrow()
                    // Replacing tasks drops their old tag assignments; start clean.
                    tagRepository.clearAll()
                }
            }
            // Restore tags and assignments. Assignments reference task ids that the
            // import just (re)created, so do this after the tasks are in place.
            val tags = backup.tags.map {
                Tag(it.id, it.name, CatppuccinAccent.fromName(it.color), it.sortOrder)
            }
            val assignments = backup.taskTags.map { it.taskId to it.tagId }
            if (tags.isNotEmpty() || assignments.isNotEmpty()) {
                tagRepository.upsertTagsAndAssignments(tags, assignments)
            }
            importedCount = tasks.size
        }
        if (importSettings) {
            backup.settings?.let { applySettings(it) }
        }
        importedCount
    }

    private suspend fun applySettings(settings: BackupSettings) {
        settings.themePalette
            ?.let { name -> ThemePalette.entries.firstOrNull { it.name == name } }
            ?.let { themeRepository.setThemePalette(it) }

        settings.accent?.let { themeRepository.setAccent(CatppuccinAccent.fromName(it)) }

        settings.sortMode?.let { taskListPreferences.setSortMode(TaskSortMode.fromName(it)) }

        settings.notifications?.let { n ->
            notificationPreferences.setPreReminderLeadMinutes(n.preReminderLeadMinutes.toSet())
            notificationPreferences.setQuietHoursEnabled(n.quietHoursEnabled)
            notificationPreferences.setQuietStartMinutes(n.quietStartMinutes)
            notificationPreferences.setQuietEndMinutes(n.quietEndMinutes)
            notificationPreferences.setSnoozeMinutes(n.snoozeMinutes)
        }
    }
}
