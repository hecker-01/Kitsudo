package dev.heckr.kitsudo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Key reused intentionally: the Catppuccin palette names (LATTE, FRAPPE, MACCHIATO, MOCHA)
// are identical to the old CatppuccinFlavor names, so no migration is needed.
private val PALETTE_KEY = stringPreferencesKey("theme_flavor")

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
}
