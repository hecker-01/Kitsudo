package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import dev.heckr.kitsudo.fake.FakeTaskRepository
import dev.heckr.kitsudo.fake.task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvanceRecurringTaskUseCaseTest {

    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun `returns null for a non-recurring task`() = runTest {
        val repo = FakeTaskRepository(listOf(task("t", deadlineAt = 1_000L)))
        val result = AdvanceRecurringTaskUseCase(repo)("t", now = 0L)
        assertNull(result)
    }

    @Test
    fun `returns null when recurring but missing a deadline`() = runTest {
        val recurring = task("t").copy(recurrenceUnit = RecurrenceUnit.DAY)
        val repo = FakeTaskRepository(listOf(recurring))
        assertNull(AdvanceRecurringTaskUseCase(repo)("t", now = 0L))
    }

    @Test
    fun `rolls deadline forward and keeps the task incomplete`() = runTest {
        val deadline = 10 * dayMs
        val recurring = task("t", deadlineAt = deadline)
            .copy(recurrenceUnit = RecurrenceUnit.DAY, recurrenceInterval = 1)
        val repo = FakeTaskRepository(listOf(recurring))

        val result = AdvanceRecurringTaskUseCase(repo)("t", now = deadline)

        assertTrue(result != null)
        val stored = repo.store.getValue("t")
        assertFalse(stored.isCompleted)
        assertTrue("deadline must move past now", stored.deadlineAt!! > deadline)
    }

    @Test
    fun `resets subtasks to incomplete for the new cycle`() = runTest {
        val recurring = task("p", deadlineAt = 10 * dayMs)
            .copy(recurrenceUnit = RecurrenceUnit.WEEK)
        val repo = FakeTaskRepository(
            listOf(
                recurring,
                task("s1", parentId = "p", isCompleted = true),
                task("s2", parentId = "p", isCompleted = true),
            ),
        )

        AdvanceRecurringTaskUseCase(repo)("p", now = 10 * dayMs)

        assertFalse(repo.store.getValue("s1").isCompleted)
        assertFalse(repo.store.getValue("s2").isCompleted)
    }

    @Test
    fun `returns null for a subtask even if marked recurring`() = runTest {
        val sub = task("s", parentId = "p", deadlineAt = 1_000L)
            .copy(recurrenceUnit = RecurrenceUnit.DAY)
        val repo = FakeTaskRepository(listOf(task("p"), sub))
        assertNull(AdvanceRecurringTaskUseCase(repo)("s", now = 0L))
    }
}
