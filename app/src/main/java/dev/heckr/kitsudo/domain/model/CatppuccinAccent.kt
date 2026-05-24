package dev.heckr.kitsudo.domain.model

/**
 * One of the 14 named accent colors from the Catppuccin palette
 * (https://catppuccin.com/palette). The same accent name exists in every
 * flavor — the actual ARGB value differs per flavor.
 *
 * Persisted as the enum [name] string under the key "accent_color" in DataStore.
 * Default is [MAUVE], which matches the previously-hardcoded primary color.
 */
enum class CatppuccinAccent {
    ROSEWATER,
    FLAMINGO,
    PINK,
    MAUVE,
    RED,
    MAROON,
    PEACH,
    YELLOW,
    GREEN,
    TEAL,
    SKY,
    SAPPHIRE,
    BLUE,
    LAVENDER,
    ;

    companion object {
        val default: CatppuccinAccent = MAUVE

        fun fromName(name: String?): CatppuccinAccent =
            entries.firstOrNull { it.name == name } ?: default
    }
}
