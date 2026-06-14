package dev.heckr.kitsudo.data.repository

import dev.heckr.kitsudo.data.local.dao.TagDao
import dev.heckr.kitsudo.data.local.entity.TaskTagCrossRef
import dev.heckr.kitsudo.data.mapper.toDomain
import dev.heckr.kitsudo.data.mapper.toEntity
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.Tag
import dev.heckr.kitsudo.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeTags().map { list -> list.map { it.toDomain() } }

    override fun observeTagsByTask(): Flow<Map<String, List<Tag>>> =
        tagDao.observeAllTaskTags().map { rows ->
            rows.groupBy({ it.taskId }, { it.tag.toDomain() })
        }

    override fun observeTagsForTask(taskId: String): Flow<List<Tag>> =
        tagDao.observeTagsForTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllTags(): List<Tag> =
        tagDao.getAllTagsOnce().map { it.toDomain() }

    override suspend fun getAllAssignments(): List<Pair<String, String>> =
        tagDao.getAllCrossRefsOnce().map { it.taskId to it.tagId }

    override suspend fun createTag(name: String, color: CatppuccinAccent): Tag {
        val tag = Tag(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            color = color,
            sortOrder = (tagDao.maxSortOrder() ?: -1) + 1,
        )
        tagDao.insertTag(tag.toEntity())
        return tag
    }

    override suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag.toEntity())

    override suspend fun deleteTag(id: String) = tagDao.deleteTag(id)

    override suspend fun setTagAssigned(taskId: String, tagId: String, assigned: Boolean) {
        if (assigned) {
            tagDao.insertCrossRef(TaskTagCrossRef(taskId, tagId))
        } else {
            tagDao.deleteCrossRef(taskId, tagId)
        }
    }

    override suspend fun setTagsForTask(taskId: String, tagIds: List<String>) {
        tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(taskId, it)) }
    }

    override suspend fun upsertTagsAndAssignments(
        tags: List<Tag>,
        assignments: List<Pair<String, String>>,
    ) {
        tagDao.insertTags(tags.map { it.toEntity() })
        tagDao.insertCrossRefs(assignments.map { (taskId, tagId) -> TaskTagCrossRef(taskId, tagId) })
    }

    override suspend fun clearAll() {
        tagDao.deleteAllCrossRefs()
        tagDao.deleteAllTags()
    }
}
