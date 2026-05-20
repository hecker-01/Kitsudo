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

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTaskById(id: String): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks WHERE parentId = :parentId")
    suspend fun deleteSubtasksByParent(parentId: String)
}
