package dev.heckr.kitsudo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.heckr.kitsudo.domain.model.TaskSortMode
import dev.heckr.kitsudo.domain.repository.TaskListPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val SORT_MODE_KEY = stringPreferencesKey("task_sort_mode")

class TaskListPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : TaskListPreferencesRepository {

    override fun observeSortMode(): Flow<TaskSortMode> = dataStore.data.map { prefs ->
        TaskSortMode.fromName(prefs[SORT_MODE_KEY])
    }

    override suspend fun setSortMode(mode: TaskSortMode) {
        dataStore.edit { prefs -> prefs[SORT_MODE_KEY] = mode.name }
    }
}
