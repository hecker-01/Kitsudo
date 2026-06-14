package dev.heckr.kitsudo.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.heckr.kitsudo.data.local.entity.TagEntity
import dev.heckr.kitsudo.data.local.entity.TaskTagCrossRef
import kotlinx.coroutines.flow.Flow

/** A tag paired with the task it is assigned to, for the list-wide tag query. */
data class TaskTagRow(
    val taskId: String,
    @Embedded val tag: TagEntity,
)

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAllTagsOnce(): List<TagEntity>

    /** Tags assigned to a single task. */
    @Query(
        "SELECT t.* FROM tags t " +
            "INNER JOIN task_tag_cross_ref cr ON cr.tagId = t.id " +
            "WHERE cr.taskId = :taskId " +
            "ORDER BY t.sortOrder ASC, t.name COLLATE NOCASE ASC",
    )
    fun observeTagsForTask(taskId: String): Flow<List<TagEntity>>

    /** Every (taskId, tag) assignment, used to attach tags to the whole list. */
    @Query(
        "SELECT cr.taskId AS taskId, t.* FROM tags t " +
            "INNER JOIN task_tag_cross_ref cr ON cr.tagId = t.id " +
            "ORDER BY t.sortOrder ASC, t.name COLLATE NOCASE ASC",
    )
    fun observeAllTaskTags(): Flow<List<TaskTagRow>>

    @Query("SELECT * FROM task_tag_cross_ref")
    suspend fun getAllCrossRefsOnce(): List<TaskTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: TaskTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<TaskTagCrossRef>)

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun deleteCrossRef(taskId: String, tagId: String)

    @Query("DELETE FROM task_tag_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("SELECT MAX(sortOrder) FROM tags")
    suspend fun maxSortOrder(): Int?
}
