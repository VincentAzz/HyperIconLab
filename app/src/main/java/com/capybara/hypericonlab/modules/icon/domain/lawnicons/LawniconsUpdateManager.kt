package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import android.content.Context
import com.capybara.hypericonlab.core.logging.AppLogStore
import com.capybara.hypericonlab.core.logging.LogType
import com.capybara.hypericonlab.modules.icon.domain.repository.AssetUpdateCheckRecord
import com.capybara.hypericonlab.modules.icon.domain.repository.AssetUpdateCheckRepository
import com.capybara.hypericonlab.modules.icon.domain.repository.AssetUpdateCheckTrigger
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 云端更新流程编排器：检查更新 → 下载 → 校验 → 解压 → 原子切换 → 清理旧版本
// 状态通过 StateFlow 暴露给 UI 观察，失败时自动回滚保持旧版本
// 代理设置从 AppSettingsRepository 读取，开启后对 github.com 资源下载加前缀加速
// 失败时通过 notifier 发系统通知（silent 模式下不发通知，用于首次启动自动拉取）
class LawniconsUpdateManager(
    private val context: Context,
    private val apiService: LawniconsApiService,
    private val downloadService: LawniconsDownloadService,
    private val resourceManager: LawniconsResourceManager,
    private val appSettingsRepository: AppSettingsRepository,
    private val notifier: LawniconsUpdateNotifier,
    private val templateManager: IconPackTemplateManager,
    private val appLogStore: AppLogStore,
    private val assetCheckRepository: AssetUpdateCheckRepository,
    appScope: CoroutineScope
) {
    // 当前更新状态，供 UI 观察
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _assetCheckState = MutableStateFlow<AssetUpdateCheckState>(
        AssetUpdateCheckState.Idle
    )
    val assetCheckState: StateFlow<AssetUpdateCheckState> = _assetCheckState.asStateFlow()

    private val _lastAutomaticAssetCheckAt = MutableStateFlow<Long?>(null)
    val lastAutomaticAssetCheckAt: StateFlow<Long?> = _lastAutomaticAssetCheckAt.asStateFlow()

    private val _lastManualAssetCheckAt = MutableStateFlow<Long?>(null)
    val lastManualAssetCheckAt: StateFlow<Long?> = _lastManualAssetCheckAt.asStateFlow()

    init {
        appScope.launch {
            restoreAssetCheckRecord(assetCheckRepository.read())
        }
    }

    // 检查更新：查询云端最新版本，与本地激活版本对比
    // silent=true 时失败不发通知（首次启动自动拉取场景）
    // 返回 ReleaseInfo（有更新）或 null（无更新或失败，state 已反映原因）
    suspend fun checkUpdate(silent: Boolean = false): ReleaseInfo? {
        val current = resourceManager.currentVersion.value
        _state.value = UpdateState.Checking(current.version)
        val release = try {
            fetchLatestRelease()
        } catch (e: LawniconsUpdateException) {
            // ApiService 已分类异常，直接映射为 Failed 状态
            failWith(e.reason, silent)
            return null
        }
        if (release == null) {
            // 接口返回 null 表示无匹配 release（非异常），归为 UNKNOWN
            failWith(FailureReason.UNKNOWN, silent)
            return null
        }
        // 版本号相同视为已是最新
        if (release.version == current.version && current.source == ResourceSource.REMOTE) {
            _state.value = UpdateState.UpToDate
            return null
        }
        _state.value = UpdateState.Idle
        return release
    }

    // 仅检查 Lawnicons 与 APK 模板更新，不下载或切换本地资源
    suspend fun checkForAssetUpdates(
        trigger: AssetUpdateCheckTrigger = AssetUpdateCheckTrigger.MANUAL,
        now: Long = System.currentTimeMillis()
    ): AssetUpdateCheckState {
        restoreAssetCheckRecord(assetCheckRepository.read())
        if (!isCheckAllowed(trigger, now)) {
            appLogStore.add("资源更新：${trigger.logName}检查仍在冷却期", LogType.INFO)
            return _assetCheckState.value
        }

        val current = resourceManager.currentVersion.value
        _assetCheckState.value = AssetUpdateCheckState.Checking(current.version)
        appLogStore.add("资源更新：开始检查可用版本", LogType.INFO)

        val release = try {
            fetchLatestRelease()
        } catch (e: LawniconsUpdateException) {
            val failed = AssetUpdateCheckState.Failed(e.reason)
            _assetCheckState.value = failed
            saveCheckRecord(trigger, now, failed)
            appLogStore.add("资源更新：版本检查失败（${e.reason}）", LogType.ERROR)
            return failed
        }

        if (release == null) {
            val failed = AssetUpdateCheckState.Failed(FailureReason.UNKNOWN)
            _assetCheckState.value = failed
            saveCheckRecord(trigger, now, failed)
            appLogStore.add("资源更新：未找到可用 Release", LogType.ERROR)
            return failed
        }

        val resourceUpdateRequired = current.source != ResourceSource.REMOTE ||
                release.version != current.version
        val templateAvailable = withContext(Dispatchers.IO) {
            current.source == ResourceSource.REMOTE && templateManager.isAvailable()
        }
        // 资源版本变化时，模板也必须跟随新版本重新准备。
        val templateUpdateRequired = release.templateArchive != null &&
                (resourceUpdateRequired || !templateAvailable)
        val result = if (resourceUpdateRequired || templateUpdateRequired) {
            AssetUpdateCheckState.Available(
                currentVersion = current,
                availableRelease = release,
                resourceUpdateRequired = resourceUpdateRequired,
                templateUpdateRequired = templateUpdateRequired,
                cacheRebuildRequired = resourceUpdateRequired
            )
        } else {
            AssetUpdateCheckState.UpToDate(current.version)
        }
        _assetCheckState.value = result
        saveCheckRecord(trigger, now, result)
        appLogStore.add(
            if (result is AssetUpdateCheckState.Available) {
                "资源更新：发现可用版本 ${release.version}"
            } else {
                "资源更新：当前资源已是最新"
            },
            LogType.INFO
        )
        return result
    }

    suspend fun markAssetUpdateCompleted() {
        val record = assetCheckRepository.read()
        val completed = AssetUpdateCheckState.UpToDate(resourceManager.currentVersion.value.version)
        assetCheckRepository.save(record.copy(state = completed))
        restoreAssetCheckRecord(record.copy(state = completed))
    }

    private suspend fun saveCheckRecord(
        trigger: AssetUpdateCheckTrigger,
        checkedAt: Long,
        state: AssetUpdateCheckState
    ) {
        val record = AssetUpdateCheckRecord(
            lastAutomaticCheckAt = if (trigger == AssetUpdateCheckTrigger.AUTOMATIC) {
                checkedAt
            } else {
                _lastAutomaticAssetCheckAt.value
            },
            lastManualCheckAt = if (trigger == AssetUpdateCheckTrigger.MANUAL) {
                checkedAt
            } else {
                _lastManualAssetCheckAt.value
            },
            state = state
        )
        assetCheckRepository.save(record)
        restoreAssetCheckRecord(record)
    }

    private suspend fun restoreAssetCheckRecord(record: AssetUpdateCheckRecord) {
        _lastAutomaticAssetCheckAt.value = record.lastAutomaticCheckAt
        _lastManualAssetCheckAt.value = record.lastManualCheckAt
        _assetCheckState.value = record.state
    }

    private fun isCheckAllowed(trigger: AssetUpdateCheckTrigger, now: Long): Boolean {
        val lastCheckedAt = when (trigger) {
            AssetUpdateCheckTrigger.AUTOMATIC -> _lastAutomaticAssetCheckAt.value
            AssetUpdateCheckTrigger.MANUAL -> _lastManualAssetCheckAt.value
        } ?: return true
        val cooldown = when (trigger) {
            AssetUpdateCheckTrigger.AUTOMATIC -> UpdateConstants.AUTOMATIC_CHECK_COOLDOWN_MS
            AssetUpdateCheckTrigger.MANUAL -> UpdateConstants.MANUAL_CHECK_COOLDOWN_MS
        }
        return now - lastCheckedAt >= cooldown
    }

    fun canCheckForAssetUpdates(
        trigger: AssetUpdateCheckTrigger = AssetUpdateCheckTrigger.MANUAL,
        now: Long = System.currentTimeMillis()
    ): Boolean = isCheckAllowed(trigger, now)

    fun assetCheckCooldownRemainingMs(
        trigger: AssetUpdateCheckTrigger = AssetUpdateCheckTrigger.MANUAL,
        now: Long = System.currentTimeMillis()
    ): Long {
        val lastCheckedAt = when (trigger) {
            AssetUpdateCheckTrigger.AUTOMATIC -> _lastAutomaticAssetCheckAt.value
            AssetUpdateCheckTrigger.MANUAL -> _lastManualAssetCheckAt.value
        } ?: return 0L
        val cooldown = when (trigger) {
            AssetUpdateCheckTrigger.AUTOMATIC -> UpdateConstants.AUTOMATIC_CHECK_COOLDOWN_MS
            AssetUpdateCheckTrigger.MANUAL -> UpdateConstants.MANUAL_CHECK_COOLDOWN_MS
        }
        return (lastCheckedAt + cooldown - now).coerceAtLeast(0L)
    }

    // 下载并安装指定 release：下载 → 校验 → 解压 → 激活 → 清理
    // silent=true 时失败不发通知（首次启动自动拉取场景）
    suspend fun downloadAndInstall(release: ReleaseInfo, silent: Boolean = false) {
        // 1. 下载 zip 到 cacheDir（失败抛 LawniconsUpdateException）
        _state.value = UpdateState.Downloading(0f)
        val zipFile = try {
            downloadService.download(release.zipUrl, release.zipSizeBytes) { progress ->
                _state.value = UpdateState.Downloading(progress)
            }
        } catch (e: LawniconsUpdateException) {
            failWith(e.reason, silent)
            return
        }

        // 2. 校验 sha256（manifest 未提供哈希时跳过校验）
        if (!downloadService.verifySha256(zipFile, release.sha256)) {
            downloadService.cleanupCache()
            failWith(FailureReason.CORRUPTED, silent)
            return
        }

        // 3. 解压到 filesDir/lawnicons_remote/<version>/（失败抛 EXTRACT_FAILED）
        _state.value = UpdateState.Extracting(0f)
        val targetDir = File(
            context.filesDir,
            "${UpdateConstants.REMOTE_BASE_DIR}/${release.version}"
        )
        // 若目录已存在（历史残留），先清理再解压
        if (targetDir.exists()) targetDir.deleteRecursively()
        try {
            downloadService.extract(zipFile, targetDir) { progress ->
                _state.value = UpdateState.Extracting(progress)
            }
        } catch (e: LawniconsUpdateException) {
            downloadService.cleanupCache()
            failWith(e.reason, silent)
            return
        }

        // 4. 原子切换：写入激活指针，刷新 provider
        val switched = resourceManager.switchToRemote(release.version)
        if (!switched) {
            targetDir.deleteRecursively()
            downloadService.cleanupCache()
            failWith(FailureReason.ACTIVATE_FAILED, silent)
            return
        }

        // 5. 清理旧版本目录（仅保留当前激活版本）
        cleanupOldVersions(release.version)

        // 6. 清理下载缓存
        downloadService.cleanupCache()

        _state.value = UpdateState.Success(release.version)
    }

    // 一键检查并安装（便捷入口，供 UI 直接调用，失败发通知）
    suspend fun checkAndInstall() {
        checkAndInstallLawnicons()
        syncTemplates()
    }

    // 静默检查并安装（首次启动自动拉取用，失败不发通知，state 仍更新供 assets tab 观察）
    suspend fun checkAndInstallSilently() {
        checkAndInstallLawniconsSilently()
        syncTemplates()
    }

    suspend fun checkAndInstallLawnicons(): Boolean {
        appLogStore.add("资源更新：开始检查 Lawnicons 版本", LogType.INFO)
        val release = checkUpdate()
        if (release != null) {
            downloadAndInstall(release)
        }
        return logResourceUpdateResult()
    }

    suspend fun checkAndInstallLawniconsSilently(): Boolean {
        appLogStore.add("初始化：开始检查 Lawnicons 版本", LogType.INFO)
        val release = checkUpdate(silent = true)
        if (release != null) {
            downloadAndInstall(release, silent = true)
        }
        return logResourceUpdateResult()
    }

    private fun logResourceUpdateResult(): Boolean {
        return when (val currentState = state.value) {
            is UpdateState.Success -> {
                appLogStore.add(
                    "资源更新：已切换到 Lawnicons ${currentState.newVersion}",
                    LogType.SUCCESS
                )
                true
            }

            UpdateState.UpToDate -> {
                appLogStore.add("资源更新：Lawnicons 已是最新版本", LogType.INFO)
                true
            }

            is UpdateState.Failed -> {
                appLogStore.add(
                    "资源更新：Lawnicons 更新失败（${currentState.reason}）",
                    LogType.ERROR
                )
                false
            }

            else -> state.value !is UpdateState.Failed
        }
    }

    // 重置状态为 Idle（UI 退出或用户确认后调用）
    fun resetState() {
        _state.value = UpdateState.Idle
    }

    // 统一失败处理：更新 state，非 silent 时触发系统通知
    private fun failWith(reason: FailureReason, silent: Boolean = false) {
        _state.value = UpdateState.Failed(reason)
        if (!silent) notifier.notifyFailed(reason)
    }

    private suspend fun fetchLatestRelease(): ReleaseInfo? {
        // 读取代理设置，开启时对 github.com 资源下载加前缀
        val useProxy = appSettingsRepository.preferencesFlow.first().useDownloadProxy
        val proxyPrefix = if (useProxy) UpdateConstants.PROXY_PREFIX else ""
        return apiService.getLatestRelease(proxyPrefix)
    }

    // 模板与当前激活的云端 Lawnicons 版本严格绑定；内置回滚资源不下载模板。
    private suspend fun syncTemplates() {
        appLogStore.add("资源更新：开始检查图标包 APK 模板", LogType.INFO)
        val available = runCatching { templateManager.ensureAvailable() }.getOrDefault(false)
        if (available) {
            appLogStore.add("资源更新：图标包 APK 模板已准备完成", LogType.SUCCESS)
        } else if (resourceManager.currentVersion.value.source == ResourceSource.REMOTE) {
            appLogStore.add("资源更新：图标包 APK 模板不可用，APK 打包已禁用", LogType.ERROR)
        } else {
            appLogStore.add("资源更新：当前使用内置资源，跳过 APK 模板", LogType.INFO)
        }
    }

    // 清理旧版本目录，保留当前激活版本
    private fun cleanupOldVersions(keepVersion: String) {
        val remoteBase = File(context.filesDir, UpdateConstants.REMOTE_BASE_DIR)
        remoteBase.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name != keepVersion) {
                dir.deleteRecursively()
            }
        }
        val templateBase = File(context.filesDir, UpdateConstants.TEMPLATE_BASE_DIR)
        templateBase.listFiles()?.forEach { dir ->
            if (dir.name != keepVersion) dir.deleteRecursively()
        }
    }

    private object UpdateConstants {
        const val REMOTE_BASE_DIR = "lawnicons_remote"
        const val TEMPLATE_BASE_DIR = "lawnicons_templates"

        // GitHub 加速代理前缀
        const val PROXY_PREFIX = "https://ghfast.top/"

        const val AUTOMATIC_CHECK_COOLDOWN_MS = 24 * 60 * 60 * 1000L
        const val MANUAL_CHECK_COOLDOWN_MS = 6 * 60 * 60 * 1000L
    }
}

private val AssetUpdateCheckTrigger.logName: String
    get() = when (this) {
        AssetUpdateCheckTrigger.AUTOMATIC -> "自动"
        AssetUpdateCheckTrigger.MANUAL -> "手动"
    }
