package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import com.capybara.hypericonlab.core.color.AppColorSchemesLoader
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.modules.build.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.UpdateState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationCacheConfig
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationPersistenceState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTask
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskStatus
import com.capybara.hypericonlab.modules.icon.domain.repository.InitializationStateRepository
import com.capybara.hypericonlab.modules.render.AppM3ColorCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class InitializationCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val manageResourcesUseCase: ManageResourcesUseCase,
    private val buildTaskManager: BuildTaskManager,
    private val assetsFacade: LawniconsAssetFacade,
    private val appM3PreprocessManager: AppM3PreprocessManager,
    private val stateRepository: InitializationStateRepository
) {
    private val _state = MutableStateFlow(InitializationState())
    val state: StateFlow<InitializationState> = _state.asStateFlow()

    private var initializationJob: Job? = null

    @Synchronized
    fun startInitialization(): Job {
        initializationJob?.takeIf { it.isActive }?.let { return it }
        return scope.launch {
            try {
                runInitialization()
            } finally {
                synchronized(this@InitializationCoordinator) {
                    initializationJob = null
                }
            }
        }.also { initializationJob = it }
    }

    fun cancelInitialization() {
        initializationJob?.cancel()
    }

    private suspend fun runInitialization() {
        val persisted = stateRepository.state.first()
        val validCompletedTasks = validCompletedTasks(persisted)
        _state.value = restoreState(persisted, validCompletedTasks)
        persistCurrentState()

        if (InitializationTask.LAWNICONS !in validCompletedTasks) {
            runLawniconsTask()
        }
        if (InitializationTask.APK_TEMPLATE !in completedTasks()) {
            runTemplateTask()
        }
        if (InitializationTask.APP_M3_CACHE !in completedTasks()) {
            runAppM3Task()
        }

        persistCurrentState()
    }

    private suspend fun runLawniconsTask() {
        markTask(InitializationTask.LAWNICONS, InitializationTaskStatus.RUNNING)
        val observer = scope.launch {
            assetsFacade.updateState.collect { updateState ->
                val progress = when (updateState) {
                    is UpdateState.Downloading -> updateState.progress
                    is UpdateState.Extracting -> updateState.progress
                    else -> 0f
                }
                markTask(InitializationTask.LAWNICONS, InitializationTaskStatus.RUNNING, progress)
            }
        }
        try {
            ensureBuiltInResources()
            val installed = assetsFacade.checkAndInstallLawniconsSilently()
            if (!installed) {
                assetsFacade.switchToAssets()
                failTask(
                    task = InitializationTask.LAWNICONS,
                    message = "云端 Lawnicons 资源拉取失败，已切换至内置资源"
                )
                return
            }
            completeTask(
                task = InitializationTask.LAWNICONS,
                message = "Lawnicons 资源已准备完成",
                resourceVersion = assetsFacade.currentVersion.value.version
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            assetsFacade.switchToAssets()
            failTask(
                task = InitializationTask.LAWNICONS,
                message = e.message ?: "Lawnicons 资源准备失败，已切换至内置资源"
            )
        } finally {
            observer.cancel()
        }
    }

    private suspend fun runTemplateTask() {
        if (assetsFacade.currentVersion.value.source != ResourceSource.REMOTE) {
            markTask(
                task = InitializationTask.APK_TEMPLATE,
                status = InitializationTaskStatus.SKIPPED,
                message = "当前使用内置资源，无需图标包 APK 模板"
            )
            return
        }

        markTask(InitializationTask.APK_TEMPLATE, InitializationTaskStatus.RUNNING)
        try {
            val available = assetsFacade.ensureTemplateAvailable { progress ->
                markTask(
                    InitializationTask.APK_TEMPLATE,
                    InitializationTaskStatus.RUNNING,
                    progress
                )
            }
            if (!available) {
                failTask(
                    task = InitializationTask.APK_TEMPLATE,
                    message = "图标包 APK 模板暂不可用，APK 打包已禁用"
                )
                return
            }
            completeTask(
                task = InitializationTask.APK_TEMPLATE,
                message = "图标包 APK 模板已准备完成",
                templateVersion = assetsFacade.currentVersion.value.version
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failTask(
                task = InitializationTask.APK_TEMPLATE,
                message = e.message ?: "图标包 APK 模板准备失败，APK 打包已禁用"
            )
        }
    }

    private suspend fun runAppM3Task() {
        markTask(InitializationTask.APP_M3_CACHE, InitializationTaskStatus.RUNNING)
        val observer = scope.launch {
            appM3PreprocessManager.state.collect { preprocessState ->
                val progress = when (preprocessState) {
                    is AppM3PreprocessManager.PreprocessState.Running ->
                        if (preprocessState.total == 0) 0f
                        else preprocessState.computed.toFloat() / preprocessState.total

                    AppM3PreprocessManager.PreprocessState.Done -> 1f
                    AppM3PreprocessManager.PreprocessState.Idle -> 0f
                }
                markTask(
                    InitializationTask.APP_M3_CACHE,
                    InitializationTaskStatus.RUNNING,
                    progress
                )
            }
        }
        try {
            val schemes = loadColorSchemes()
            buildTaskManager.updateAppColorSchemes(schemes)
            appM3PreprocessManager.updateAppColorSchemes(schemes)
            appM3PreprocessManager.preprocessNow()
            completeTask(
                task = InitializationTask.APP_M3_CACHE,
                message = "颜色映射缓存已生成",
                cacheSourceVersion = assetsFacade.currentVersion.value.version,
                cacheConfigVersion = InitializationCacheConfig.VERSION
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failTask(
                task = InitializationTask.APP_M3_CACHE,
                message = e.message ?: "颜色映射缓存生成失败"
            )
        } finally {
            observer.cancel()
        }
    }

    private suspend fun ensureBuiltInResources() {
        val lawniconsDir = File(context.filesDir, RESOURCE_DIRECTORY)
        if (!lawniconsDir.exists() || lawniconsDir.list()?.isEmpty() == true) {
            manageResourcesUseCase.performUnzip()
        }
        assetsFacade.refresh()
    }

    private suspend fun loadColorSchemes(): Map<String, Pair<String, String>> {
        val schemes = runCatching {
            assetsFacade.getProvider()
                .openColorSchemes(COLOR_SCHEMES_FILE)
                .use { input -> AppColorSchemesLoader.loadFromStream(input) }
        }.getOrDefault(emptyMap())
        AppM3ColorCache.loadFromFile(
            context = context,
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ThemeColorSpec.SPEC_2021
        )
        return schemes
    }

    private fun completedTasks(): Set<InitializationTask> = _state.value.tasks
        .filter { it.status == InitializationTaskStatus.COMPLETED }
        .mapTo(mutableSetOf()) { it.task }

    private fun markTask(
        task: InitializationTask,
        status: InitializationTaskStatus,
        progress: Float = 0f,
        message: String? = null
    ) {
        val tasks = _state.value.tasks.map { current ->
            if (current.task == task) {
                InitializationTaskState(task, status, progress, message)
            } else {
                current
            }
        }
        _state.value = _state.value.copy(
            tasks = tasks,
            activeTask = tasks.firstOrNull { it.status == InitializationTaskStatus.RUNNING }?.task,
            isCompleted = tasks.all { it.status == InitializationTaskStatus.COMPLETED }
        )
    }

    private suspend fun completeTask(
        task: InitializationTask,
        message: String,
        resourceVersion: String? = null,
        templateVersion: String? = null,
        cacheSourceVersion: String? = null,
        cacheConfigVersion: String? = null
    ) {
        markTask(task, InitializationTaskStatus.COMPLETED, 1f, message)
        _state.value = _state.value.copy(
            resourceVersion = resourceVersion ?: _state.value.resourceVersion,
            templateVersion = templateVersion ?: _state.value.templateVersion,
            cacheSourceVersion = cacheSourceVersion ?: _state.value.cacheSourceVersion,
            cacheConfigVersion = cacheConfigVersion ?: _state.value.cacheConfigVersion,
            failedTask = if (_state.value.failedTask == task) null else _state.value.failedTask,
            failureMessage = if (_state.value.failedTask == task) null else _state.value.failureMessage,
            failureAt = if (_state.value.failedTask == task) null else _state.value.failureAt
        )
        persistCurrentState()
    }

    private suspend fun failTask(task: InitializationTask, message: String) {
        markTask(task, InitializationTaskStatus.FAILED, message = message)
        _state.value = _state.value.copy(
            failedTask = task,
            failureMessage = message,
            failureAt = System.currentTimeMillis()
        )
        persistCurrentState()
    }

    private suspend fun persistCurrentState() {
        val current = _state.value
        stateRepository.save(
            InitializationPersistenceState(
                completedTasks = completedTasks(),
                activeTask = current.activeTask,
                isCompleted = current.isCompleted,
                resourceVersion = current.resourceVersion,
                templateVersion = current.templateVersion,
                cacheSourceVersion = current.cacheSourceVersion,
                cacheConfigVersion = current.cacheConfigVersion,
                failedTask = current.failedTask,
                failureMessage = current.failureMessage,
                failureAt = current.failureAt
            )
        )
    }

    private fun restoreState(
        persisted: InitializationPersistenceState,
        validCompletedTasks: Set<InitializationTask>
    ): InitializationState {
        val tasks = InitializationTask.entries.map { task ->
            val status = when {
                task in validCompletedTasks -> InitializationTaskStatus.COMPLETED
                task == persisted.failedTask -> InitializationTaskStatus.FAILED
                else -> InitializationTaskStatus.PENDING
            }
            InitializationTaskState(
                task = task,
                status = status,
                progress = if (status == InitializationTaskStatus.COMPLETED) 1f else 0f,
                message = if (task == persisted.failedTask) persisted.failureMessage else null
            )
        }
        return InitializationState(
            tasks = tasks,
            isCompleted = tasks.all { it.status == InitializationTaskStatus.COMPLETED },
            resourceVersion = persisted.resourceVersion,
            templateVersion = persisted.templateVersion,
            cacheSourceVersion = persisted.cacheSourceVersion,
            cacheConfigVersion = persisted.cacheConfigVersion,
            failedTask = persisted.failedTask,
            failureMessage = persisted.failureMessage,
            failureAt = persisted.failureAt
        )
    }

    private fun validCompletedTasks(
        persisted: InitializationPersistenceState
    ): Set<InitializationTask> {
        val currentVersion = assetsFacade.currentVersion.value.version
        return persisted.completedTasks.filterTo(mutableSetOf()) { task ->
            when (task) {
                InitializationTask.LAWNICONS -> persisted.resourceVersion == currentVersion
                InitializationTask.APK_TEMPLATE ->
                    persisted.templateVersion == currentVersion &&
                            assetsFacade.templateState.value is IconPackTemplateState.Available

                InitializationTask.APP_M3_CACHE ->
                    persisted.cacheSourceVersion == currentVersion &&
                            persisted.isCacheConfigCurrent()
            }
        }
    }

    private companion object {
        const val RESOURCE_DIRECTORY = "lawnicons"
        const val COLOR_SCHEMES_FILE = "app_color_schemes.xml"
    }
}
