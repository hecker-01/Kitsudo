package dev.heckr.kitsudo.data.repository

import dev.heckr.kitsudo.data.local.dao.TaskDao
import dev.heckr.kitsudo.data.mapper.toDomain
import dev.heckr.kitsudo.data.mapper.toEntity
import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
) : TaskRepository {

    override fun getTopLevelTasksWithSubtasks(): Flow<List<TaskWithSubtasks>> =
        taskDao.getTopLevelTasksWithSubtasks().map { list -> list.map { it.toDomain() } }

    override fun getSubtasks(parentId: String): Flow<List<Task>> =
        taskDao.getSubtasks(parentId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSubtasksOnce(parentId: String): List<Task> =
        taskDao.getSubtasksOnce(parentId).map { it.toDomain() }

    override suspend fun getTaskById(id: String): Task? =
        taskDao.getTaskById(id)?.toDomain()

    override fun observeTask(id: String): Flow<Task?> =
        taskDao.observeTaskById(id).map { it?.toDomain() }

    override suspend fun createTask(task: Task): Result<Unit> = runCatching {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task): Result<Unit> = runCatching {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: String): Result<Unit> = runCatching {
        taskDao.deleteTaskById(id)
    }

    override suspend fun deleteSubtasks(parentId: String): Result<Unit> = runCatching {
        taskDao.deleteSubtasksByParent(parentId)
    }
}
