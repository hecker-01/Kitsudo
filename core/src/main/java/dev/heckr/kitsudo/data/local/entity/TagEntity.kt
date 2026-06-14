package dev.heckr.kitsudo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** CatppuccinAccent name (e.g. "MAUVE"). Resolved to a Color at render time. */
    val color: String,
    /** Ascending display order in pickers and the filter row. */
    val sortOrder: Int = 0,
)
