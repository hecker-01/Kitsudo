package dev.heckr.kitsudo.domain.repository

import dev.heckr.kitsudo.domain.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Reads/writes the user's global notification preferences. The flow emits the
 * latest snapshot on every change, mirroring [ThemeRepository] semantics.
 */
interface NotificationPreferencesRepository {
    fun observe(): Flow<NotificationPreferences>

    suspend fun setPreReminderLeadMinutes(minutes: Int)
    suspend fun setQuietHoursEnabled(enabled: Boolean)
    suspend fun setQuietStartMinutes(minutes: Int)
    suspend fun setQuietEndMinutes(minutes: Int)
    suspend fun setSnoozeMinutes(minutes: Int)
}
