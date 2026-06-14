package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.NotificationPreferences
import dev.heckr.kitsudo.domain.model.TaskSortMode
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.fake.FakeNotificationPreferencesRepository
import dev.heckr.kitsudo.fake.FakeTaskListPreferencesRepository
import dev.heckr.kitsudo.fake.FakeTaskRepository
import dev.heckr.kitsudo.fake.FakeThemeRepository
import dev.heckr.kitsudo.fake.task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRoundTripTest {

    private val sourceTasks = listOf(
        task("p", title = "Parent", sortOrder = 0),
        task("a", title = "Child A", parentId = "p", isCompleted = true),
        task("b", title = "Child B", parentId = "p"),
    )

    private fun exporter(
        tasks: FakeTaskRepository,
        theme: FakeThemeRepository,
        notif: FakeNotificationPreferencesRepository,
        sort: FakeTaskListPreferencesRepository,
    ) = ExportTasksUseCase(tasks, theme, notif, sort)

    private fun importer(
        tasks: FakeTaskRepository,
        theme: FakeThemeRepository,
        notif: FakeNotificationPreferencesRepository,
        sort: FakeTaskListPreferencesRepository,
    ) = ImportTasksUseCase(tasks, theme, notif, sort)

    @Test
    fun `export then import restores tasks and settings`() = runTest {
        val json = exporter(
            FakeTaskRepository(sourceTasks),
            FakeThemeRepository(palette = ThemePalette.LATTE, accent = CatppuccinAccent.default),
            FakeNotificationPreferencesRepository(
                NotificationPreferences(
                    preReminderLeadMinutes = setOf(15, 60),
                    quietHoursEnabled = true,
                    snoozeMinutes = 30,
                ),
            ),
            FakeTaskListPreferencesRepository(TaskSortMode.DEADLINE),
        ).invoke()

        val tasks = FakeTaskRepository()
        val theme = FakeThemeRepository()
        val notif = FakeNotificationPreferencesRepository()
        val sort = FakeTaskListPreferencesRepository()

        val result = importer(tasks, theme, notif, sort).invoke(
            json, importTasks = true, importSettings = true, mode = ImportTasksUseCase.Mode.REPLACE,
        )

        assertEquals(3, result.getOrNull())
        assertEquals(setOf("p", "a", "b"), tasks.store.keys)
        assertEquals(ThemePalette.LATTE, theme.palette)
        assertEquals(setOf(15, 60), notif.prefs.preReminderLeadMinutes)
        assertTrue(notif.prefs.quietHoursEnabled)
        assertEquals(30, notif.prefs.snoozeMinutes)
        assertEquals(TaskSortMode.DEADLINE, sort.mode)
    }

    @Test
    fun `replace import wipes existing tasks first`() = runTest {
        val json = exporter(
            FakeTaskRepository(listOf(task("only"))),
            FakeThemeRepository(), FakeNotificationPreferencesRepository(), FakeTaskListPreferencesRepository(),
        ).invoke()

        val tasks = FakeTaskRepository(listOf(task("old1"), task("old2")))
        importer(tasks, FakeThemeRepository(), FakeNotificationPreferencesRepository(), FakeTaskListPreferencesRepository())
            .invoke(json, importTasks = true, importSettings = false, mode = ImportTasksUseCase.Mode.REPLACE)

        assertEquals(setOf("only"), tasks.store.keys)
    }

    @Test
    fun `merge import keeps existing tasks`() = runTest {
        val json = exporter(
            FakeTaskRepository(listOf(task("new"))),
            FakeThemeRepository(), FakeNotificationPreferencesRepository(), FakeTaskListPreferencesRepository(),
        ).invoke()

        val tasks = FakeTaskRepository(listOf(task("existing")))
        importer(tasks, FakeThemeRepository(), FakeNotificationPreferencesRepository(), FakeTaskListPreferencesRepository())
            .invoke(json, importTasks = true, importSettings = false, mode = ImportTasksUseCase.Mode.MERGE)

        assertEquals(setOf("existing", "new"), tasks.store.keys)
    }

    @Test
    fun `settings-only import leaves tasks untouched`() = runTest {
        val json = exporter(
            FakeTaskRepository(listOf(task("backed-up"))),
            FakeThemeRepository(palette = ThemePalette.FRAPPE),
            FakeNotificationPreferencesRepository(), FakeTaskListPreferencesRepository(TaskSortMode.NEWEST),
        ).invoke()

        val tasks = FakeTaskRepository(listOf(task("current")))
        val theme = FakeThemeRepository()
        val sort = FakeTaskListPreferencesRepository()
        importer(tasks, theme, FakeNotificationPreferencesRepository(), sort)
            .invoke(json, importTasks = false, importSettings = true, mode = ImportTasksUseCase.Mode.REPLACE)

        assertEquals(setOf("current"), tasks.store.keys) // unchanged
        assertEquals(ThemePalette.FRAPPE, theme.palette)
        assertEquals(TaskSortMode.NEWEST, sort.mode)
    }

    @Test
    fun `malformed json fails without throwing`() = runTest {
        val result = importer(
            FakeTaskRepository(), FakeThemeRepository(),
            FakeNotificationPreferencesRepository(), FakeTaskListPreferencesRepository(),
        ).invoke("definitely not json", importTasks = true, importSettings = true, mode = ImportTasksUseCase.Mode.REPLACE)

        assertTrue(result.isFailure)
    }
}
