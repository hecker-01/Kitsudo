package dev.heckr.kitsudo.domain.model

/**
 * A reusable, color-coded label that can be attached to top-level tasks
 * (many-to-many). The color is one of the named [CatppuccinAccent] hues so tags
 * stay on-palette across every theme flavor.
 */
data class Tag(
    val id: String,
    val name: String,
    val color: CatppuccinAccent,
    /** Ascending display order in pickers and filter rows. */
    val sortOrder: Int = 0,
)
