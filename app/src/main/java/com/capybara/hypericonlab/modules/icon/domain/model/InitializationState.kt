package com.capybara.hypericonlab.modules.icon.domain.model

enum class InitializationTask {
    LAWNICONS,
    APK_TEMPLATE,
    APP_M3_CACHE
}

enum class InitializationTaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    SKIPPED,
    FAILED
}

object InitializationCacheConfig {
    const val VERSION = "tonal_spot_spec_2021_v1"
}

data class InitializationTaskState(
    val task: InitializationTask,
    val status: InitializationTaskStatus = InitializationTaskStatus.PENDING,
    val progress: Float = 0f,
    val message: String? = null
)

data class AssetUpdateUiState(
    val currentVersion: String,
    val availableVersion: String,
    val tasks: List<InitializationTaskState>,
    val isRunning: Boolean = true
)

data class InitializationState(
    val tasks: List<InitializationTaskState> = InitializationTask.entries.map {
        InitializationTaskState(task = it)
    },
    val activeTask: InitializationTask? = null,
    val isCompleted: Boolean = false,
    val resourceVersion: String? = null,
    val templateVersion: String? = null,
    val cacheSourceVersion: String? = null,
    val cacheConfigVersion: String? = null,
    val failedTask: InitializationTask? = null,
    val failureMessage: String? = null,
    val failureAt: Long? = null,
    val requiresManualStart: Boolean = false
)

data class InitializationPersistenceState(
    val completedTasks: Set<InitializationTask> = emptySet(),
    val activeTask: InitializationTask? = null,
    val isCompleted: Boolean = false,
    val resourceVersion: String? = null,
    val templateVersion: String? = null,
    val cacheSourceVersion: String? = null,
    val cacheConfigVersion: String? = null,
    val failedTask: InitializationTask? = null,
    val failureMessage: String? = null,
    val failureAt: Long? = null,
    val requiresManualStart: Boolean = false
) {
    fun isCacheConfigCurrent(): Boolean =
        cacheConfigVersion == InitializationCacheConfig.VERSION
}
