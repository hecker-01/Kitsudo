package dev.heckr.kitsudo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.heckr.kitsudo.data.local.entity.TaskEntity
import dev.heckr.kitsudo.data.local.entity.TaskWithSubtasksEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Transaction
    @Query(
        "SELECT * FROM tasks WHERE parentId IS NULL " +
            "ORDER BY sortOrder ASC, createdAt DESC",
    )
    fun getTopLevelTasksWithSubtasks(): Flow<List<TaskWithSubtasksEntity>>

    @Query(
        "SELECT * FROM tasks WHERE parentId = :parentId " +
            "ORDER BY sortOrder ASC, createdAt ASC",
    )
    fun getSubtasks(parentId: String): Flow<List<TaskEntity>>

    /** One-shot snapshot of a parent's subtasks (used to capture state before deletion). */
    @Query(
        "SELECT * FROM tasks WHERE parentId = :parentId " +
            "ORDER BY sortOrder ASC, createdAt ASC",
    )
    suspend fun getSubtasksOnce(parentId: String): List<TaskEntity>

    /** Every task (parents + subtasks), used for backup export. */
    @Query("SELECT * FROM tasks ORDER BY parentId IS NOT NULL, sortOrder ASC, createdAt ASC")
    suspend fun getAllOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTaskById(id: String): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    /**
     * Sets [completed] on a top-level task **and every one of its subtasks** in a
     * single statement, so a cascade completion is one atomic write (and therefore
     * one Flow emission / one Wear snapshot push) instead of N sequential updates.
     */
    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id OR parentId = :id")
    suspend fun setCompletedForTaskAndSubtasks(id: String, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks WHERE parentId = :parentId")
    suspend fun deleteSubtasksByParent(parentId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    /**
     * Atomically swaps the entire tasks table for [tasks]. Used by the Wear
     * snapshot sync: wrapping the clear+insert in a transaction means a process
     * kill mid-swap can never leave the watch with a half-applied (or empty) list.
     */
    @Transaction
    suspend fun replaceAll(tasks: List<TaskEntity>) {
        deleteAll()
        insertAll(tasks)
    }
}
