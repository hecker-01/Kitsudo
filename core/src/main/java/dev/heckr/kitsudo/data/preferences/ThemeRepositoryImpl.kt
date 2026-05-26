package dev.heckr.kitsudo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.M3WearColors
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Key reused intentionally: the Catppuccin palette names (LATTE, FRAPPE, MACCHIATO, MOCHA)
// are identical to the old CatppuccinFlavor names, so no migration is needed.
private val PALETTE_KEY = stringPreferencesKey("theme_flavor")
private val ACCENT_KEY  = stringPreferencesKey("accent_color")

// Material You dynamic colors (only populated when phone uses MATERIAL3 theme).
// Stored as packed ARGB ints — same bit layout as android.graphics.Color / Color.toArgb().
private val M3_PRIMARY        = intPreferencesKey("m3_primary")
private val M3_ON_PRIMARY     = intPreferencesKey("m3_on_primary")
private val M3_TERTIARY       = intPreferencesKey("m3_tertiary")
private val M3_ERROR          = intPreferencesKey("m3_error")
private val M3_ON_SURFACE     = intPreferencesKey("m3_on_surface")
private val M3_ON_SURF_VAR    = intPreferencesKey("m3_on_surface_variant")
private val M3_OUTLINE        = intPreferencesKey("m3_outline")
private val M3_SURF_LOW       = intPreferencesKey("m3_surf_low")
private val M3_SURF_MID       = intPreferencesKey("m3_surf_mid")
private val M3_SURF_HIGH      = intPreferencesKey("m3_surf_high")

class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    override fun getThemePalette(): Flow<ThemePalette> =
        dataStore.data.map { prefs ->
            prefs[PALETTE_KEY]
                ?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() }
                ?: ThemePalette.MOCHA
        }

    override suspend fun setThemePalette(palette: ThemePalette) {
        dataStore.edit { prefs -> prefs[PALETTE_KEY] = palette.name }
    }

    override fun getAccent(): Flow<CatppuccinAccent> =
        dataStore.data.map { prefs ->
            CatppuccinAccent.fromName(prefs[ACCENT_KEY])
        }

    override suspend fun setAccent(accent: CatppuccinAccent) {
        dataStore.edit { prefs -> prefs[ACCENT_KEY] = accent.name }
    }

    override fun getM3Colors(): Flow<M3WearColors?> = dataStore.data.map { prefs ->
        val primary = prefs[M3_PRIMARY]     ?: return@map null
        val onPrimary = prefs[M3_ON_PRIMARY] ?: return@map null
        val tertiary = prefs[M3_TERTIARY]   ?: return@map null
        val error = prefs[M3_ERROR]         ?: return@map null
        val onSurface = prefs[M3_ON_SURFACE] ?: return@map null
        val onSurfVar = prefs[M3_ON_SURF_VAR] ?: return@map null
        val outline = prefs[M3_OUTLINE]     ?: return@map null
        val surfLow = prefs[M3_SURF_LOW]    ?: return@map null
        val surfMid = prefs[M3_SURF_MID]    ?: return@map null
        val surfHigh = prefs[M3_SURF_HIGH]  ?: return@map null
        M3WearColors(
            primary = primary,
            onPrimary = onPrimary,
            tertiary = tertiary,
            error = error,
            onSurface = onSurface,
            onSurfaceVariant = onSurfVar,
            outline = outline,
            surfaceContainerLow = surfLow,
            surfaceContainer = surfMid,
            surfaceContainerHigh = surfHigh,
        )
    }

    override suspend fun setM3Colors(colors: M3WearColors) {
        dataStore.edit { prefs ->
            prefs[M3_PRIMARY]     = colors.primary
            prefs[M3_ON_PRIMARY]  = colors.onPrimary
            prefs[M3_TERTIARY]    = colors.tertiary
            prefs[M3_ERROR]       = colors.error
            prefs[M3_ON_SURFACE]  = colors.onSurface
            prefs[M3_ON_SURF_VAR] = colors.onSurfaceVariant
            prefs[M3_OUTLINE]     = colors.outline
            prefs[M3_SURF_LOW]    = colors.surfaceContainerLow
            prefs[M3_SURF_MID]    = colors.surfaceContainer
            prefs[M3_SURF_HIGH]   = colors.surfaceContainerHigh
        }
    }
}
