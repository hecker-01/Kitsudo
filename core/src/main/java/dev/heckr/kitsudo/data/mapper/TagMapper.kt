package dev.heckr.kitsudo.data.mapper

import dev.heckr.kitsudo.data.local.entity.TagEntity
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.Tag

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    color = CatppuccinAccent.fromName(color),
    sortOrder = sortOrder,
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    color = color.name,
    sortOrder = sortOrder,
)
