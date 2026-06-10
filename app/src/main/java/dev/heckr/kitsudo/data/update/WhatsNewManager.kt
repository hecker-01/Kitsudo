package dev.heckr.kitsudo.data.update

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Surfaces a "What's New" sheet the first time the app is launched after an
 * install or update.
 *
 * It keys off the package's [lastUpdateTime], which the system bumps on every
 * (re)install - including reinstalls of the same version - so the sheet re-shows
 * after each install rather than only on a version bump. The timestamp of the
 * last install we showed for is persisted; any change shows the sheet once, then
 * records it so it never repeats for that install. The changelog is pulled from
 * the matching GitHub release (by tag) so it always reflects what's installed and
 * needs no manual in-app sync.
 */
@Singleton
class WhatsNewManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    sealed interface State {
        data object Hidden : State
        data object Loading : State
        data class Shown(val version: String, val notes: String?) : State
    }

    private val _state = MutableStateFlow<State>(State.Hidden)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Call once per launch. Shows the sheet whenever the app's install timestamp
     * differs from the one it was last shown for, then records it so the sheet
     * appears at most once per install.
     */
    suspend fun maybeShowOnLaunch() {
        val lastUpdate = lastUpdateTime() ?: return
        val shownFor = dataStore.data.first()[LAST_SHOWN_UPDATE_TIME]

        // Already shown for this install -> nothing to do.
        if (shownFor == lastUpdate) return

        // Record up front so the sheet appears at most once per install.
        dataStore.edit { it[LAST_SHOWN_UPDATE_TIME] = lastUpdate }

        val version = BuildConfig.VERSION_NAME.substringBefore("-")
        _state.value = State.Loading
        val notes = UpdateChecker.fetchReleaseNotes(BuildConfig.VERSION_NAME)
        _state.value = State.Shown(version = version, notes = notes)
    }

    fun dismiss() {
        _state.value = State.Hidden
    }

    /** Millis of the last install/update, or null if the package can't be read. */
    private fun lastUpdateTime(): Long? = try {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .lastUpdateTime
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private companion object {
        val LAST_SHOWN_UPDATE_TIME = longPreferencesKey("last_shown_update_time")
    }
}
