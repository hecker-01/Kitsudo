package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.fake.FakeTaskRepository
import dev.heckr.kitsudo.fake.task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTaskUseCaseTest {

    private fun useCase(repo: FakeTaskRepository) =
        CreateTaskUseCase(repo, RecomputeParentCompletionUseCase(repo))

    @Test
    fun `creates a top-level task`() = runTest {
        val repo = FakeTaskRepository()
        val result = useCase(repo)(title = "  Buy milk  ")
        assertTrue(result.isSuccess)
        val created = repo.store.values.single()
        assertEquals("Buy milk", created.title) // trimmed
        assertEquals(null, created.parentId)
    }

    @Test
    fun `rejects nesting deeper than two levels`() = runTest {
        val repo = FakeTaskRepository(
            listOf(
                task("p"),
                task("sub", parentId = "p"),
            ),
        )
        // Attempt to add a child of a task that is itself a subtask.
        val result = useCase(repo)(title = "deep", parentId = "sub")
        assertTrue(result.isFailure)
        assertEquals(2, repo.store.size) // nothing added
    }

    @Test
    fun `adding an incomplete subtask un-completes a completed parent`() = runTest {
        val repo = FakeTaskRepository(listOf(task("p", isCompleted = true)))
        useCase(repo)(title = "child", parentId = "p")
        assertFalse(repo.store.getValue("p").isCompleted)
    }

    @Test
    fun `fails when parent does not exist`() = runTest {
        val repo = FakeTaskRepository()
        val result = useCase(repo)(title = "child", parentId = "ghost")
        assertTrue(result.isFailure)
    }
}
