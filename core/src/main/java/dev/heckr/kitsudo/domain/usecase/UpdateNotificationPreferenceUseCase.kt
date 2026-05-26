package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import javax.inject.Inject

/**
 * Thin wrapper around the repository's individual setters. Bundling them here
 * means ViewModels only inject one use case instead of five separate ones.
 */
class UpdateNotificationPreferenceUseCase @Inject constructor(
    private val repository: NotificationPreferencesRepository,
) {
    suspend fun setPreReminderLeadMinutes(minutes: Int) =
        repository.setPreReminderLeadMinutes(minutes)

    suspend fun setQuietHoursEnabled(enabled: Boolean) =
        repository.setQuietHoursEnabled(enabled)

    suspend fun setQuietStartMinutes(minutes: Int) =
        repository.setQuietStartMinutes(minutes)

    suspend fun setQuietEndMinutes(minutes: Int) =
        repository.setQuietEndMinutes(minutes)

    suspend fun setSnoozeMinutes(minutes: Int) =
        repository.setSnoozeMinutes(minutes)
}
