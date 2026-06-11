package dev.heckr.kitsudo.data.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the one-time welcome onboarding has been seen.
 *
 * Unlike [dev.heckr.kitsudo.data.update.WhatsNewManager], which re-shows on every
 * install, onboarding is shown exactly once for the lifetime of the install: a
 * single boolean flag in DataStore that flips to true the first time the user
 * finishes (or skips) the flow and never resets.
 */
@Singleton
class OnboardingManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** Emits true once onboarding has been completed (or skipped). */
    val isComplete: Flow<Boolean> = dataStore.data.map { it[COMPLETED] ?: false }

    /** Records that onboarding is finished so it never shows again. */
    suspend fun markComplete() {
        dataStore.edit { it[COMPLETED] = true }
    }

    private companion object {
        val COMPLETED = booleanPreferencesKey("onboarding_complete")
    }
}
