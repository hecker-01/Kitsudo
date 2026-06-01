package dev.heckr.kitsudo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.heckr.kitsudo.domain.model.NotificationPreferences
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Legacy single-value key (pre-multi-reminder). Read once for migration.
private val LEAD_KEY = intPreferencesKey("notif_lead_min")
private val LEAD_SET_KEY = stringSetPreferencesKey("notif_lead_mins_set")
private val QUIET_ENABLED_KEY = booleanPreferencesKey("notif_quiet_enabled")
private val QUIET_START_KEY = intPreferencesKey("notif_quiet_start_min")
private val QUIET_END_KEY = intPreferencesKey("notif_quiet_end_min")
private val SNOOZE_KEY = intPreferencesKey("notif_snooze_min")

class NotificationPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : NotificationPreferencesRepository {

    override fun observe(): Flow<NotificationPreferences> = dataStore.data.map { prefs ->
        val defaults = NotificationPreferences()
        val leadSet = prefs[LEAD_SET_KEY]?.mapNotNull { it.toIntOrNull() }?.toSet()
            // Migrate the old single int value (0 meant "none") if the set was never written.
            ?: prefs[LEAD_KEY]?.let { legacy -> if (legacy > 0) setOf(legacy) else emptySet() }
            ?: defaults.preReminderLeadMinutes
        NotificationPreferences(
            preReminderLeadMinutes = leadSet,
            quietHoursEnabled = prefs[QUIET_ENABLED_KEY] ?: defaults.quietHoursEnabled,
            quietStartMinutes = prefs[QUIET_START_KEY] ?: defaults.quietStartMinutes,
            quietEndMinutes = prefs[QUIET_END_KEY] ?: defaults.quietEndMinutes,
            snoozeMinutes = prefs[SNOOZE_KEY] ?: defaults.snoozeMinutes,
        )
    }

    override suspend fun setPreReminderLeadMinutes(minutes: Set<Int>) {
        dataStore.edit { prefs ->
            prefs[LEAD_SET_KEY] = minutes.filter { it > 0 }.map { it.toString() }.toSet()
            // Drop the stale legacy key so it can't override the set on later reads.
            prefs.remove(LEAD_KEY)
        }
    }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { it[QUIET_ENABLED_KEY] = enabled }
    }

    override suspend fun setQuietStartMinutes(minutes: Int) {
        dataStore.edit { it[QUIET_START_KEY] = minutes.coerceIn(0, MINUTES_PER_DAY - 1) }
    }

    override suspend fun setQuietEndMinutes(minutes: Int) {
        dataStore.edit { it[QUIET_END_KEY] = minutes.coerceIn(0, MINUTES_PER_DAY - 1) }
    }

    override suspend fun setSnoozeMinutes(minutes: Int) {
        dataStore.edit { it[SNOOZE_KEY] = minutes.coerceAtLeast(1) }
    }

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
