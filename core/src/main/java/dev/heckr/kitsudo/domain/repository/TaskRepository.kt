package dev.heckr.kitsudo.domain.repository

import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    /** Emits top-level tasks (parentId == null) together with their subtasks. */
    fun getTopLevelTasksWithSubtasks(): Flow<List<TaskWithSubtasks>>

    /** Emits subtasks for a given parent task id. */
    fun getSubtasks(parentId: String): Flow<List<Task>>

    /** One-shot snapshot of a parent's subtasks (for capturing state before deletion). */
    suspend fun getSubtasksOnce(parentId: String): List<Task>

    suspend fun getTaskById(id: String): Task?
    fun observeTask(id: String): Flow<Task?>

    /** One-shot snapshot of every task (parents + subtasks), for backup export. */
    suspend fun getAllTasks(): List<Task>

    /** Upserts [tasks] (merge import): existing ids are updated, new ones added. */
    suspend fun insertTasks(tasks: List<Task>): Result<Unit>

    /** Atomically replaces the whole table with [tasks] (replace import). */
    suspend fun replaceAllTasks(tasks: List<Task>): Result<Unit>

    suspend fun createTask(task: Task): Result<Unit>
    suspend fun updateTask(task: Task): Result<Unit>

    /** Atomically sets completion on a top-level task and all of its subtasks. */
    suspend fun setCompletedForTaskAndSubtasks(id: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTask(id: String): Result<Unit>
    suspend fun deleteSubtasks(parentId: String): Result<Unit>
}
