package dev.heckr.kitsudo.domain.model

/**
 * The full set of theme choices available in the app.
 *
 * [MATERIAL3] applies Material You dynamic colors derived from the system wallpaper (API 31+,
 * always available since min SDK is 33). The remaining values map 1-to-1 with
 * Catppuccin flavors.
 *
 * Persisted as the enum [name] string. The Catppuccin names are kept intentionally
 * compatible with the old "theme_flavor" DataStore key so existing preferences migrate
 * automatically.
 */
enum class ThemePalette {
    MATERIAL3,
    LATTE,
    FRAPPE,
    MACCHIATO,
    MOCHA;

    /** Returns the matching [CatppuccinFlavor], or null when this is [MATERIAL3]. */
    fun toCatppuccinFlavor(): CatppuccinFlavor? = when (this) {
        MATERIAL3 -> null
        LATTE -> CatppuccinFlavor.LATTE
        FRAPPE -> CatppuccinFlavor.FRAPPE
        MACCHIATO -> CatppuccinFlavor.MACCHIATO
        MOCHA -> CatppuccinFlavor.MOCHA
    }

    val isCatppuccin: Boolean get() = this != MATERIAL3
    val isDark: Boolean get() = this == FRAPPE || this == MACCHIATO || this == MOCHA
}
