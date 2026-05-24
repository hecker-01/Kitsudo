package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.NotificationPreferences
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotificationPreferencesUseCase @Inject constructor(
    private val repository: NotificationPreferencesRepository,
) {
    operator fun invoke(): Flow<NotificationPreferences> = repository.observe()
}
