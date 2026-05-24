package dev.heckr.kitsudo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dev.heckr.kitsudo.domain.model.NotificationPreferences
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val LEAD_KEY = intPreferencesKey("notif_lead_min")
private val QUIET_ENABLED_KEY = booleanPreferencesKey("notif_quiet_enabled")
private val QUIET_START_KEY = intPreferencesKey("notif_quiet_start_min")
private val QUIET_END_KEY = intPreferencesKey("notif_quiet_end_min")
private val SNOOZE_KEY = intPreferencesKey("notif_snooze_min")

class NotificationPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : NotificationPreferencesRepository {

    override fun observe(): Flow<NotificationPreferences> = dataStore.data.map { prefs ->
        val defaults = NotificationPreferences()
        NotificationPreferences(
            preReminderLeadMinutes = prefs[LEAD_KEY] ?: defaults.preReminderLeadMinutes,
            quietHoursEnabled = prefs[QUIET_ENABLED_KEY] ?: defaults.quietHoursEnabled,
            quietStartMinutes = prefs[QUIET_START_KEY] ?: defaults.quietStartMinutes,
            quietEndMinutes = prefs[QUIET_END_KEY] ?: defaults.quietEndMinutes,
            snoozeMinutes = prefs[SNOOZE_KEY] ?: defaults.snoozeMinutes,
        )
    }

    override suspend fun setPreReminderLeadMinutes(minutes: Int) {
        dataStore.edit { it[LEAD_KEY] = minutes.coerceAtLeast(0) }
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
