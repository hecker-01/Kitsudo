package dev.heckr.kitsudo.domain.repository

import dev.heckr.kitsudo.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    /** All tags, ordered for display. */
    fun observeTags(): Flow<List<Tag>>

    /** Map of task id -> its assigned tags, for decorating the whole task list. */
    fun observeTagsByTask(): Flow<Map<String, List<Tag>>>

    /** Tags assigned to a single task. */
    fun observeTagsForTask(taskId: String): Flow<List<Tag>>

    /** One-shot list of every tag (for backup export). */
    suspend fun getAllTags(): List<Tag>

    /** One-shot list of every (taskId, tagId) assignment (for backup export). */
    suspend fun getAllAssignments(): List<Pair<String, String>>

    suspend fun createTag(name: String, color: dev.heckr.kitsudo.domain.model.CatppuccinAccent): Tag
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(id: String)

    /** Adds or removes a single task<->tag assignment. */
    suspend fun setTagAssigned(taskId: String, tagId: String, assigned: Boolean)

    /** Replaces a task's full set of assigned tags. */
    suspend fun setTagsForTask(taskId: String, tagIds: List<String>)

    /** Bulk restore for import: upsert tags and assignments. */
    suspend fun upsertTagsAndAssignments(tags: List<Tag>, assignments: List<Pair<String, String>>)

    /** Wipe all tags and assignments (replace import). */
    suspend fun clearAll()
}
