package dev.heckr.kitsudo.presentation.tasks

import dev.heckr.kitsudo.domain.model.Priority
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.TaskSortMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSortTest {

    private fun item(
        id: String,
        isCompleted: Boolean = false,
        deadlineAt: Long? = null,
        overdue: Boolean = false,
        highPriority: Boolean = false,
        createdAt: Long = 0L,
        sortOrder: Int = 0,
    ) = TaskWithSubtasksUi(
        id = id,
        title = id,
        description = "",
        isCompleted = isCompleted,
        deadlineAt = deadlineAt,
        isDeadlineOverdue = overdue,
        syncStatus = SyncStatus.SYNCED,
        subtasks = emptyList(),
        priority = if (highPriority) Priority.HIGH else Priority.NORMAL,
        createdAt = createdAt,
        sortOrder = sortOrder,
    )

    private fun List<TaskWithSubtasksUi>.ids() = map { it.id }

    @Test
    fun `smart sort orders overdue, upcoming, no-deadline, then completed`() {
        val list = listOf(
            item("done", isCompleted = true),
            item("noDeadline"),
            item("upcoming", deadlineAt = 5_000),
            item("overdue", deadlineAt = 1_000, overdue = true),
        )

        val sorted = list.sortedByMode(TaskSortMode.SMART).ids()

        assertEquals(listOf("overdue", "upcoming", "noDeadline", "done"), sorted)
    }

    @Test
    fun `smart sort breaks no-deadline ties by priority`() {
        val list = listOf(
            item("normal", createdAt = 1),
            item("high", highPriority = true, createdAt = 2),
        )

        val sorted = list.sortedByMode(TaskSortMode.SMART).ids()

        assertEquals(listOf("high", "normal"), sorted)
    }

    @Test
    fun `alphabetical sort sinks completed to the bottom`() {
        val list = listOf(
            item("Charlie"),
            item("alpha"),
            item("Bravo", isCompleted = true),
        )

        val sorted = list.sortedByMode(TaskSortMode.ALPHABETICAL).ids()

        // case-insensitive within incomplete, completed last
        assertEquals(listOf("alpha", "Charlie", "Bravo"), sorted)
    }

    @Test
    fun `custom sort follows sortOrder`() {
        val list = listOf(
            item("third", sortOrder = 2),
            item("first", sortOrder = 0),
            item("second", sortOrder = 1),
        )

        val sorted = list.sortedByMode(TaskSortMode.CUSTOM).ids()

        assertEquals(listOf("first", "second", "third"), sorted)
    }

    @Test
    fun `deadline sort puts soonest first and no-deadline last`() {
        val list = listOf(
            item("none"),
            item("later", deadlineAt = 9_000),
            item("soon", deadlineAt = 1_000),
        )

        val sorted = list.sortedByMode(TaskSortMode.DEADLINE).ids()

        assertEquals(listOf("soon", "later", "none"), sorted)
    }
}
