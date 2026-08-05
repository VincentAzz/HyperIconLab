package com.capybara.hypericonlab.modules.icon.data.repository

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationPersistenceState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTask
import com.capybara.hypericonlab.modules.icon.domain.repository.InitializationStateRepository
import com.capybara.hypericonlab.modules.settings.data.local.AppDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InitializationStateRepositoryImpl(
    private val appDataStore: AppDataStore
) : InitializationStateRepository {

    override val state: Flow<InitializationPersistenceState> =
        appDataStore.data.map { preferences ->
            InitializationPersistenceState(
                completedTasks = parseTasks(preferences[AppDataStore.INITIALIZATION_COMPLETED_TASKS]),
                activeTask = parseTask(preferences[AppDataStore.INITIALIZATION_ACTIVE_TASK]),
                isCompleted = preferences[AppDataStore.INITIALIZATION_COMPLETED] ?: false,
                resourceVersion = preferences[AppDataStore.INITIALIZATION_RESOURCE_VERSION],
                templateVersion = preferences[AppDataStore.INITIALIZATION_TEMPLATE_VERSION],
                cacheSourceVersion = preferences[AppDataStore.INITIALIZATION_CACHE_SOURCE_VERSION],
                cacheConfigVersion = preferences[AppDataStore.INITIALIZATION_CACHE_CONFIG_VERSION],
                failedTask = parseTask(preferences[AppDataStore.INITIALIZATION_FAILED_TASK]),
                failureMessage = preferences[AppDataStore.INITIALIZATION_FAILURE_MESSAGE],
                failureAt = preferences[AppDataStore.INITIALIZATION_FAILURE_AT],
                requiresManualStart =
                    preferences[AppDataStore.INITIALIZATION_REQUIRES_MANUAL_START] ?: false
            )
        }

    override suspend fun save(state: InitializationPersistenceState) {
        appDataStore.edit { preferences ->
            preferences[AppDataStore.INITIALIZATION_COMPLETED_TASKS] =
                state.completedTasks.joinToString(TASK_SEPARATOR) { it.name }
            putTask(preferences, AppDataStore.INITIALIZATION_ACTIVE_TASK, state.activeTask)
            preferences[AppDataStore.INITIALIZATION_COMPLETED] = state.isCompleted
            putOptionalString(
                preferences,
                AppDataStore.INITIALIZATION_RESOURCE_VERSION,
                state.resourceVersion
            )
            putOptionalString(
                preferences,
                AppDataStore.INITIALIZATION_TEMPLATE_VERSION,
                state.templateVersion
            )
            putOptionalString(
                preferences,
                AppDataStore.INITIALIZATION_CACHE_SOURCE_VERSION,
                state.cacheSourceVersion
            )
            putOptionalString(
                preferences,
                AppDataStore.INITIALIZATION_CACHE_CONFIG_VERSION,
                state.cacheConfigVersion
            )
            putTask(preferences, AppDataStore.INITIALIZATION_FAILED_TASK, state.failedTask)
            putOptionalString(
                preferences,
                AppDataStore.INITIALIZATION_FAILURE_MESSAGE,
                state.failureMessage
            )
            putOptionalLong(preferences, AppDataStore.INITIALIZATION_FAILURE_AT, state.failureAt)
            preferences[AppDataStore.INITIALIZATION_REQUIRES_MANUAL_START] =
                state.requiresManualStart
        }
    }

    override suspend fun clear() {
        appDataStore.edit { preferences ->
            preferences.remove(AppDataStore.INITIALIZATION_COMPLETED_TASKS)
            preferences.remove(AppDataStore.INITIALIZATION_ACTIVE_TASK)
            preferences.remove(AppDataStore.INITIALIZATION_COMPLETED)
            preferences.remove(AppDataStore.INITIALIZATION_RESOURCE_VERSION)
            preferences.remove(AppDataStore.INITIALIZATION_TEMPLATE_VERSION)
            preferences.remove(AppDataStore.INITIALIZATION_CACHE_SOURCE_VERSION)
            preferences.remove(AppDataStore.INITIALIZATION_CACHE_CONFIG_VERSION)
            preferences.remove(AppDataStore.INITIALIZATION_FAILED_TASK)
            preferences.remove(AppDataStore.INITIALIZATION_FAILURE_MESSAGE)
            preferences.remove(AppDataStore.INITIALIZATION_FAILURE_AT)
            preferences.remove(AppDataStore.INITIALIZATION_REQUIRES_MANUAL_START)
        }
    }

    private fun parseTasks(value: String?): Set<InitializationTask> = value
        ?.split(TASK_SEPARATOR)
        ?.mapNotNull(::parseTask)
        ?.toSet()
        ?: emptySet()

    private fun parseTask(value: String?): InitializationTask? = value
        ?.takeIf { it.isNotBlank() }
        ?.let { name -> runCatching { InitializationTask.valueOf(name) }.getOrNull() }

    private fun putTask(
        preferences: MutablePreferences,
        key: Preferences.Key<String>,
        task: InitializationTask?
    ) {
        if (task == null) preferences.remove(key) else preferences[key] = task.name
    }

    private fun putOptionalString(
        preferences: MutablePreferences,
        key: Preferences.Key<String>,
        value: String?
    ) {
        if (value == null) preferences.remove(key) else preferences[key] = value
    }

    private fun putOptionalLong(
        preferences: MutablePreferences,
        key: Preferences.Key<Long>,
        value: Long?
    ) {
        if (value == null) preferences.remove(key) else preferences[key] = value
    }

    companion object {
        private const val TASK_SEPARATOR = ","
    }
}
