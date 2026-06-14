package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.Tag
import dev.heckr.kitsudo.domain.repository.TagRepository
import javax.inject.Inject

/**
 * Creates a tag, reusing an existing one when a tag with the same (case-insensitive)
 * name already exists so the list stays free of duplicates.
 */
class CreateTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(name: String, color: CatppuccinAccent): Result<Tag> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be blank"))
        }
        val existing = repository.getAllTags()
            .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return Result.success(existing)
        return Result.success(repository.createTag(trimmed, color))
    }
}
