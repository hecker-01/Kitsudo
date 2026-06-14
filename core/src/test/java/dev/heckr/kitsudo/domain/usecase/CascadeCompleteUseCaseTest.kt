package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.fake.FakeTaskRepository
import dev.heckr.kitsudo.fake.task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CascadeCompleteUseCaseTest {

    private fun useCase(repo: FakeTaskRepository) =
        CascadeCompleteUseCase(repo, CompleteTaskUseCase(repo))

    @Test
    fun `completing a parent completes all its subtasks`() = runTest {
        val repo = FakeTaskRepository(
            listOf(
                task("p"),
                task("a", parentId = "p"),
                task("b", parentId = "p"),
            ),
        )

        useCase(repo)("p", isCompleted = true)

        assertTrue(repo.store.getValue("p").isCompleted)
        assertTrue(repo.store.getValue("a").isCompleted)
        assertTrue(repo.store.getValue("b").isCompleted)
    }

    @Test
    fun `un-completing a parent un-completes all its subtasks`() = runTest {
        val repo = FakeTaskRepository(
            listOf(
                task("p", isCompleted = true),
                task("a", parentId = "p", isCompleted = true),
                task("b", parentId = "p", isCompleted = true),
            ),
        )

        useCase(repo)("p", isCompleted = false)

        assertFalse(repo.store.getValue("p").isCompleted)
        assertFalse(repo.store.getValue("a").isCompleted)
        assertFalse(repo.store.getValue("b").isCompleted)
    }

    @Test
    fun `completing the last subtask auto-completes the parent`() = runTest {
        val repo = FakeTaskRepository(
            listOf(
                task("p"),
                task("a", parentId = "p", isCompleted = true),
                task("b", parentId = "p", isCompleted = false),
            ),
        )

        useCase(repo)("b", isCompleted = true)

        assertTrue(repo.store.getValue("b").isCompleted)
        assertTrue("parent should auto-complete", repo.store.getValue("p").isCompleted)
    }

    @Test
    fun `completing one of several subtasks leaves the parent incomplete`() = runTest {
        val repo = FakeTaskRepository(
            listOf(
                task("p"),
                task("a", parentId = "p", isCompleted = false),
                task("b", parentId = "p", isCompleted = false),
            ),
        )

        useCase(repo)("a", isCompleted = true)

        assertTrue(repo.store.getValue("a").isCompleted)
        assertFalse(repo.store.getValue("p").isCompleted)
    }

    @Test
    fun `un-completing a subtask un-completes a completed parent`() = runTest {
        val repo = FakeTaskRepository(
            listOf(
                task("p", isCompleted = true),
                task("a", parentId = "p", isCompleted = true),
                task("b", parentId = "p", isCompleted = true),
            ),
        )

        useCase(repo)("a", isCompleted = false)

        assertFalse(repo.store.getValue("a").isCompleted)
        assertFalse(repo.store.getValue("p").isCompleted)
        assertTrue("sibling stays complete", repo.store.getValue("b").isCompleted)
    }

    @Test
    fun `missing task is a no-op`() = runTest {
        val repo = FakeTaskRepository(listOf(task("p")))
        useCase(repo)("does-not-exist", isCompleted = true)
        assertEquals(1, repo.store.size)
        assertFalse(repo.store.getValue("p").isCompleted)
    }
}
