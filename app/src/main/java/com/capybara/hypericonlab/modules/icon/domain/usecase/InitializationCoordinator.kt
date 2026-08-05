package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import com.capybara.hypericonlab.core.color.AppColorSchemesLoader
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.logging.AppLogStore
import com.capybara.hypericonlab.core.logging.LogType
import com.capybara.hypericonlab.modules.build.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.AssetUpdateCheckState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.UpdateState
import com.capybara.hypericonlab.modules.icon.domain.model.AssetUpdateUiState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationCacheConfig
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationPersistenceState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTask
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskStatus
import com.capybara.hypericonlab.modules.icon.domain.repository.AssetUpdateCheckTrigger
import com.capybara.hypericonlab.modules.icon.domain.repository.InitializationServiceController
import com.capybara.hypericonlab.modules.icon.domain.repository.InitializationStateRepository
import com.capybara.hypericonlab.modules.render.AppM3ColorCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
    private val stateRepository: InitializationStateRepository,
    private val appLogStore: AppLogStore,
    private val serviceController: InitializationServiceController
) {
    private val _state = MutableStateFlow(InitializationState())
    val state: StateFlow<InitializationState> = _state.asStateFlow()

    private val _appColorSchemes = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val appColorSchemes: StateFlow<Map<String, Pair<String, String>>> =
        _appColorSchemes.asStateFlow()

    private val _resourcesReadyVersion = MutableStateFlow<String?>(null)
    val resourcesReadyVersion: StateFlow<String?> = _resourcesReadyVersion.asStateFlow()

    private val _assetUpdateRunning = MutableStateFlow(false)
    val assetUpdateRunning: StateFlow<Boolean> = _assetUpdateRunning.asStateFlow()

    private val _assetUpdateState = MutableStateFlow<AssetUpdateUiState?>(null)
    val assetUpdateState: StateFlow<AssetUpdateUiState?> = _assetUpdateState.asStateFlow()

    private var initializationJob: Job? = null
    private var resetJob: Job? = null
    private var assetUpdateJob: Job? = null

    @Synchronized
    fun startInitialization(manualStart: Boolean = false): Job {
        initializationJob?.takeIf { it.isActive }?.let { return it }
        return scope.launch {
            try {
                resetJob?.join()
                resetJob = null
                runInitialization(manualStart)
            } finally {
                synchronized(this@InitializationCoordinator) {
                    initializationJob = null
                }
            }
        }.also { initializationJob = it }
    }

    fun cancelInitialization() {
        initializationJob?.cancel()
        serviceController.stop()
    }

    /** 按资源、模板、颜色缓存顺序执行资产页的手动更新。 */
    @Synchronized
    fun startManualAssetUpdate(): Job {
        assetUpdateJob?.takeIf { it.isActive }?.let { return it }
        return scope.launch {
            _assetUpdateRunning.value = true
            val available = assetsFacade.assetCheckState.value as? AssetUpdateCheckState.Available
            _assetUpdateState.value = createAssetUpdateState(available)
            val observer = scope.launch {
                launch {
                    assetsFacade.updateState.collect { updateState ->
                        updateAssetResourceTask(updateState)
                    }
                }
                launch {
                    assetsFacade.templateState.collect { templateState ->
                        updateAssetTemplateTask(templateState)
                    }
                }
            }
            try {
                appLogStore.add("资源更新：开始执行手动资产更新", LogType.INFO)
                assetsFacade.resetUpdateState()
                assetsFacade.checkAndInstall()
                val resourceUpdated = assetsFacade.updateState.value is UpdateState.Success
                if (assetsFacade.updateState.value !is UpdateState.Failed) {
                    completeAssetUpdateTask(InitializationTask.LAWNICONS)
                }
                completeAssetTemplateTask()
                assetsFacade.refresh()
                if (resourceUpdated) {
                    appM3PreprocessManager.clearCache()
                }
                loadCurrentColorSchemes()
                markAssetUpdateTask(
                    task = InitializationTask.APP_M3_CACHE,
                    status = InitializationTaskStatus.RUNNING
                )
                appM3PreprocessManager.preprocessNow()
                completeAssetUpdateTask(InitializationTask.APP_M3_CACHE)
                if (_assetUpdateState.value?.tasks?.none {
                        it.status == InitializationTaskStatus.FAILED
                    } == true
                ) {
                    assetsFacade.markAssetUpdateCompleted()
                    _assetUpdateState.value = null
                }
                appLogStore.add("资源更新：App-M3 颜色映射缓存生成完成", LogType.SUCCESS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appLogStore.add(
                    "资源更新：手动资产更新失败（${e.message ?: "未知错误"}）",
                    LogType.ERROR
                )
            } finally {
                observer.cancelAndJoin()
                _assetUpdateState.value = _assetUpdateState.value?.copy(isRunning = false)
                _assetUpdateRunning.value = false
                synchronized(this@InitializationCoordinator) {
                    assetUpdateJob = null
                }
            }
        }.also { assetUpdateJob = it }
    }

    private fun createAssetUpdateState(
        available: AssetUpdateCheckState.Available?
    ): AssetUpdateUiState {
        val currentVersion = available?.currentVersion?.version
            ?: assetsFacade.currentVersion.value.version
        val availableVersion = available?.availableRelease?.version ?: currentVersion
        val resourceStatus = if (available?.resourceUpdateRequired == false) {
            InitializationTaskStatus.COMPLETED
        } else {
            InitializationTaskStatus.RUNNING
        }
        val templateStatus = if (available?.templateUpdateRequired == false) {
            InitializationTaskStatus.COMPLETED
        } else {
            InitializationTaskStatus.PENDING
        }
        return AssetUpdateUiState(
            currentVersion = currentVersion,
            availableVersion = availableVersion,
            tasks = InitializationTask.entries.map { task ->
                InitializationTaskState(
                    task = task,
                    status = when (task) {
                        InitializationTask.LAWNICONS -> resourceStatus
                        InitializationTask.APK_TEMPLATE -> templateStatus
                        InitializationTask.APP_M3_CACHE -> InitializationTaskStatus.PENDING
                    },
                    progress = if (task == InitializationTask.LAWNICONS &&
                        resourceStatus == InitializationTaskStatus.COMPLETED
                    ) 1f else 0f
                )
            }
        )
    }

    private fun updateAssetResourceTask(updateState: UpdateState) {
        when (updateState) {
            is UpdateState.Checking -> markAssetUpdateTask(
                InitializationTask.LAWNICONS,
                InitializationTaskStatus.RUNNING
            )

            is UpdateState.Downloading -> markAssetUpdateTask(
                InitializationTask.LAWNICONS,
                InitializationTaskStatus.RUNNING,
                updateState.progress
            )

            is UpdateState.Extracting -> markAssetUpdateTask(
                InitializationTask.LAWNICONS,
                InitializationTaskStatus.RUNNING,
                updateState.progress
            )

            UpdateState.UpToDate,
            is UpdateState.Success -> completeAssetUpdateTask(InitializationTask.LAWNICONS)

            is UpdateState.Failed -> markAssetUpdateTask(
                InitializationTask.LAWNICONS,
                InitializationTaskStatus.FAILED,
                message = "Lawnicons 资源更新失败"
            )

            UpdateState.Idle -> Unit
        }
    }

    private fun updateAssetTemplateTask(templateState: IconPackTemplateState) {
        when (templateState) {
            IconPackTemplateState.Checking -> markAssetUpdateTask(
                InitializationTask.APK_TEMPLATE,
                InitializationTaskStatus.RUNNING
            )

            is IconPackTemplateState.Downloading -> markAssetUpdateTask(
                InitializationTask.APK_TEMPLATE,
                InitializationTaskStatus.RUNNING,
                templateState.progress
            )

            is IconPackTemplateState.Available -> completeAssetTemplateTask()
            IconPackTemplateState.Unavailable,
            IconPackTemplateState.Failed -> markAssetUpdateTask(
                InitializationTask.APK_TEMPLATE,
                InitializationTaskStatus.FAILED,
                message = "图标包 APK 模板更新失败"
            )

            IconPackTemplateState.Idle -> Unit
        }
    }

    private fun completeAssetTemplateTask() {
        if (assetsFacade.currentVersion.value.source == ResourceSource.REMOTE &&
            assetsFacade.templateState.value is IconPackTemplateState.Available
        ) {
            completeAssetUpdateTask(InitializationTask.APK_TEMPLATE)
        }
    }

    private fun completeAssetUpdateTask(task: InitializationTask) {
        markAssetUpdateTask(task, InitializationTaskStatus.COMPLETED, 1f)
    }

    private fun markAssetUpdateTask(
        task: InitializationTask,
        status: InitializationTaskStatus,
        progress: Float = 0f,
        message: String? = null
    ) {
        val current = _assetUpdateState.value ?: return
        val tasks = current.tasks.map { taskState ->
            if (taskState.task == task) {
                taskState.copy(
                    status = status,
                    progress = progress.coerceIn(0f, 1f),
                    message = message
                )
            } else {
                taskState
            }
        }
        _assetUpdateState.value = current.copy(tasks = tasks)
    }

    fun resetForManualInitialization(
        invalidatedTasks: Set<InitializationTask> = InitializationTask.entries.toSet()
    ) {
        initializationJob?.cancel()
        assetUpdateJob?.cancel()
        appM3PreprocessManager.cancelPreprocess()
        val currentState = _state.value
        val resetTasks = currentState.tasks.map { taskState ->
            if (taskState.task in invalidatedTasks ||
                taskState.status == InitializationTaskStatus.RUNNING
            ) {
                InitializationTaskState(task = taskState.task)
            } else {
                taskState
            }
        }
        _state.value = currentState.copy(
            tasks = resetTasks,
            activeTask = null,
            isCompleted = resetTasks.all {
                it.status == InitializationTaskStatus.COMPLETED
            },
            resourceVersion = currentState.resourceVersion.takeUnless {
                InitializationTask.LAWNICONS in invalidatedTasks
            },
            templateVersion = currentState.templateVersion.takeUnless {
                InitializationTask.APK_TEMPLATE in invalidatedTasks
            },
            cacheSourceVersion = currentState.cacheSourceVersion.takeUnless {
                InitializationTask.APP_M3_CACHE in invalidatedTasks
            },
            cacheConfigVersion = currentState.cacheConfigVersion.takeUnless {
                InitializationTask.APP_M3_CACHE in invalidatedTasks
            },
            failedTask = null,
            failureMessage = null,
            failureAt = null,
            requiresManualStart = true
        )
        appLogStore.add("初始化已重置，等待手动开始", LogType.INFO)
        resetJob = scope.launch {
            persistCurrentState()
        }
    }

    private suspend fun runInitialization(manualStart: Boolean) {
        val persisted = stateRepository.state.first()
        val validCompletedTasks = validCompletedTasks(persisted)
        val wasCompleted = persisted.isCompleted &&
                validCompletedTasks.size == InitializationTask.entries.size
        if (validCompletedTasks.isEmpty()) {
            appLogStore.add("开始初始化资源", LogType.INFO)
        } else if (validCompletedTasks.size < InitializationTask.entries.size) {
            appLogStore.add("继续未完成的初始化任务", LogType.INFO)
        }
        _state.value = restoreState(persisted, validCompletedTasks)
        if (persisted.requiresManualStart && !manualStart) {
            persistCurrentState()
            return
        }
        _state.value = _state.value.copy(requiresManualStart = false)
        persistCurrentState()
        serviceController.start()

        if (InitializationTask.LAWNICONS in validCompletedTasks) {
            loadCurrentColorSchemes()
            publishResourcesReady()
        }

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
        if (_state.value.isCompleted && !wasCompleted) {
            appLogStore.add("初始化完成", LogType.SUCCESS)
        }
        if (wasCompleted && !manualStart && assetUpdateJob?.isActive != true) {
            runAutomaticAssetCheck()
        }
    }

    // 非首次启动仅检查远程资产，不自动下载或切换资源。
    private suspend fun runAutomaticAssetCheck() {
        if (!assetsFacade.canCheckForAssetUpdates(AssetUpdateCheckTrigger.AUTOMATIC)) {
            appLogStore.add("启动检查：自动检查处于冷却期，保留已有结果", LogType.INFO)
            return
        }
        val result = assetsFacade.checkForAssetUpdates(AssetUpdateCheckTrigger.AUTOMATIC)
        when (result) {
            is AssetUpdateCheckState.Available -> appLogStore.add(
                "启动检查：发现资产更新 ${result.currentVersion.version} → " +
                        result.availableRelease.version,
                LogType.INFO
            )

            is AssetUpdateCheckState.UpToDate -> appLogStore.add(
                "启动检查：资产已是最新版本 ${result.currentVersion}",
                LogType.INFO
            )

            is AssetUpdateCheckState.Failed -> appLogStore.add(
                "启动检查：资产更新检查失败（${result.reason}）",
                LogType.ERROR
            )

            AssetUpdateCheckState.Idle,
            is AssetUpdateCheckState.Checking -> Unit
        }
    }

    private suspend fun runLawniconsTask() {
        appLogStore.add("初始化：开始准备 Lawnicons 资源", LogType.INFO)
        markTask(InitializationTask.LAWNICONS, InitializationTaskStatus.RUNNING)
        val observer = scope.launch {
            assetsFacade.updateState.collect { updateState ->
                val progress = when (updateState) {
                    is UpdateState.Downloading ->
                        updateState.progress * LAWNICONS_DOWNLOAD_WEIGHT

                    is UpdateState.Extracting ->
                        LAWNICONS_DOWNLOAD_WEIGHT +
                                updateState.progress * LAWNICONS_EXTRACT_WEIGHT

                    UpdateState.UpToDate,
                    is UpdateState.Success -> 1f
                    else -> 0f
                }
                markTask(InitializationTask.LAWNICONS, InitializationTaskStatus.RUNNING, progress)
            }
        }
        try {
            ensureBuiltInResources()
            val installed = assetsFacade.checkAndInstallLawniconsSilently()
            if (!installed) {
                observer.cancelAndJoin()
                assetsFacade.switchToAssets()
                loadCurrentColorSchemes()
                publishResourcesReady()
                failTask(
                    task = InitializationTask.LAWNICONS,
                    message = "云端 Lawnicons 资源拉取失败，已切换至内置资源"
                )
                appLogStore.add("初始化：Lawnicons 拉取失败，已切换至内置资源", LogType.ERROR)
                return
            }
            observer.cancelAndJoin()
            completeTask(
                task = InitializationTask.LAWNICONS,
                message = "Lawnicons 资源已准备完成",
                resourceVersion = assetsFacade.currentVersion.value.version
            )
            loadCurrentColorSchemes()
            publishResourcesReady()
            appLogStore.add("初始化：Lawnicons 资源准备完成", LogType.SUCCESS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            observer.cancelAndJoin()
            assetsFacade.switchToAssets()
            loadCurrentColorSchemes()
            publishResourcesReady()
            failTask(
                task = InitializationTask.LAWNICONS,
                message = e.message ?: "Lawnicons 资源准备失败，已切换至内置资源"
            )
            appLogStore.add("初始化：Lawnicons 资源准备失败，已切换至内置资源", LogType.ERROR)
        } finally {
            observer.cancelAndJoin()
        }
    }

    private suspend fun runTemplateTask() {
        if (assetsFacade.currentVersion.value.source != ResourceSource.REMOTE) {
            markTask(
                task = InitializationTask.APK_TEMPLATE,
                status = InitializationTaskStatus.SKIPPED,
                message = "当前使用内置资源，无需图标包 APK 模板"
            )
            appLogStore.add("初始化：当前使用内置资源，跳过 APK 模板", LogType.INFO)
            return
        }

        appLogStore.add("初始化：开始准备图标包 APK 模板", LogType.INFO)
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
            appLogStore.add("初始化：图标包 APK 模板准备完成", LogType.SUCCESS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failTask(
                task = InitializationTask.APK_TEMPLATE,
                message = e.message ?: "图标包 APK 模板准备失败，APK 打包已禁用"
            )
            appLogStore.add("初始化：图标包 APK 模板准备失败，APK 打包已禁用", LogType.ERROR)
        }
    }

    private suspend fun runAppM3Task() {
        appLogStore.add("初始化：开始生成 App-M3 颜色映射缓存", LogType.INFO)
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
            loadCurrentColorSchemes()
            appM3PreprocessManager.preprocessNow()
            observer.cancelAndJoin()
            completeTask(
                task = InitializationTask.APP_M3_CACHE,
                message = "颜色映射缓存已生成",
                cacheSourceVersion = assetsFacade.currentVersion.value.version,
                cacheConfigVersion = InitializationCacheConfig.VERSION
            )
            appLogStore.add("初始化：App-M3 颜色映射缓存生成完成", LogType.SUCCESS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            observer.cancelAndJoin()
            failTask(
                task = InitializationTask.APP_M3_CACHE,
                message = e.message ?: "颜色映射缓存生成失败"
            )
            appLogStore.add("初始化：App-M3 颜色映射缓存生成失败", LogType.ERROR)
        } finally {
            observer.cancelAndJoin()
        }
    }

    private suspend fun ensureBuiltInResources() {
        val lawniconsDir = File(context.filesDir, RESOURCE_DIRECTORY)
        if (!lawniconsDir.exists() || lawniconsDir.list()?.isEmpty() == true) {
            manageResourcesUseCase.performUnzip { }
        }
        assetsFacade.refresh()
        loadCurrentColorSchemes()
        publishResourcesReady()
    }

    private suspend fun loadCurrentColorSchemes() {
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
        _appColorSchemes.value = schemes
        buildTaskManager.updateAppColorSchemes(schemes)
        appM3PreprocessManager.updateAppColorSchemes(schemes)
    }

    private fun publishResourcesReady() {
        _resourcesReadyVersion.value = assetsFacade.currentVersion.value.version
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
                failureAt = current.failureAt,
                requiresManualStart = current.requiresManualStart
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
            failureAt = persisted.failureAt,
            requiresManualStart = persisted.requiresManualStart
        )
    }

    private fun validCompletedTasks(
        persisted: InitializationPersistenceState
    ): Set<InitializationTask> {
        val currentVersionInfo = assetsFacade.currentVersion.value
        val currentVersion = currentVersionInfo.version
        val usingBuiltInAssets = currentVersionInfo.source == ResourceSource.ASSETS
        return persisted.completedTasks.filterTo(mutableSetOf()) { task ->
            when (task) {
                InitializationTask.LAWNICONS -> usingBuiltInAssets ||
                        persisted.resourceVersion == currentVersion

                InitializationTask.APK_TEMPLATE ->
                    usingBuiltInAssets ||
                            (persisted.templateVersion == currentVersion &&
                                    assetsFacade.isTemplateAvailable())

                InitializationTask.APP_M3_CACHE ->
                    persisted.isCacheConfigCurrent() &&
                            (usingBuiltInAssets || persisted.cacheSourceVersion == currentVersion ||
                                    InitializationTask.LAWNICONS !in persisted.completedTasks)
            }
        }
    }

    private companion object {
        const val RESOURCE_DIRECTORY = "lawnicons"
        const val COLOR_SCHEMES_FILE = "app_color_schemes.xml"
        const val LAWNICONS_DOWNLOAD_WEIGHT = 0.5f
        const val LAWNICONS_EXTRACT_WEIGHT = 0.5f
    }
}
