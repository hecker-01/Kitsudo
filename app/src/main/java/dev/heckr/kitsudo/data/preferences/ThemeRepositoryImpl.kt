package dev.heckr.kitsudo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val FLAVOR_KEY = stringPreferencesKey("theme_flavor")

class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    override fun getThemeFlavor(): Flow<CatppuccinFlavor> =
        dataStore.data.map { prefs ->
            prefs[FLAVOR_KEY]
                ?.let { runCatching { CatppuccinFlavor.valueOf(it) }.getOrNull() }
                ?: CatppuccinFlavor.MOCHA
        }

    override suspend fun setThemeFlavor(flavor: CatppuccinFlavor) {
        dataStore.edit { prefs -> prefs[FLAVOR_KEY] = flavor.name }
    }
}
