package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import android.content.Context
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.File

// 云端更新流程编排器：检查更新 → 下载 → 校验 → 解压 → 原子切换 → 清理旧版本
// 状态通过 StateFlow 暴露给 UI 观察，失败时自动回滚保持旧版本
// 代理设置从 AppSettingsRepository 读取，开启后对 github.com 资源下载加前缀加速
class LawniconsUpdateManager(
    private val context: Context,
    private val apiService: LawniconsApiService,
    private val downloadService: LawniconsDownloadService,
    private val resourceManager: LawniconsResourceManager,
    private val appSettingsRepository: AppSettingsRepository
) {
    // 当前更新状态，供 UI 观察
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    // 检查更新：查询云端最新版本，与本地激活版本对比
    // 返回 ReleaseInfo（有更新）或 null（无更新或失败，state 已反映原因）
    suspend fun checkUpdate(): ReleaseInfo? {
        val current = resourceManager.currentVersion.value
        _state.value = UpdateState.Checking(current.version)
        // 读取代理设置，开启时对 github.com 资源下载加前缀
        val useProxy = appSettingsRepository.preferencesFlow.first().useDownloadProxy
        val proxyPrefix = if (useProxy) UpdateConstants.PROXY_PREFIX else ""
        val release = apiService.getLatestRelease(proxyPrefix)
        if (release == null) {
            _state.value = UpdateState.Failed(UpdateConstants.MSG_FETCH_FAILED)
            return null
        }
        // 版本号相同视为已是最新
        if (release.version == current.version) {
            _state.value = UpdateState.UpToDate
            return null
        }
        _state.value = UpdateState.Idle
        return release
    }

    // 下载并安装指定 release：下载 → 校验 → 解压 → 激活 → 清理
    suspend fun downloadAndInstall(release: ReleaseInfo) {
        // 1. 下载 zip 到 cacheDir
        _state.value = UpdateState.Downloading(0f)
        val zipFile = downloadService.download(release.zipUrl, release.zipSizeBytes) { progress ->
            _state.value = UpdateState.Downloading(progress)
        } ?: run {
            _state.value = UpdateState.Failed(UpdateConstants.MSG_DOWNLOAD_FAILED)
            return
        }

        // 2. 校验 sha256（manifest 未提供哈希时跳过）
        if (!downloadService.verifySha256(zipFile, release.sha256)) {
            downloadService.cleanupCache()
            _state.value = UpdateState.Failed(UpdateConstants.MSG_VERIFY_FAILED)
            return
        }

        // 3. 解压到 filesDir/lawnicons_remote/<version>/
        _state.value = UpdateState.Extracting(0f)
        val targetDir = File(
            context.filesDir,
            "${UpdateConstants.REMOTE_BASE_DIR}/${release.version}"
        )
        // 若目录已存在（历史残留），先清理再解压
        if (targetDir.exists()) targetDir.deleteRecursively()
        val success = downloadService.extract(zipFile, targetDir) { progress ->
            _state.value = UpdateState.Extracting(progress)
        }
        if (!success) {
            downloadService.cleanupCache()
            _state.value = UpdateState.Failed(UpdateConstants.MSG_EXTRACT_FAILED)
            return
        }

        // 4. 原子切换：写入激活指针，刷新 provider
        val switched = resourceManager.switchToRemote(release.version)
        if (!switched) {
            targetDir.deleteRecursively()
            downloadService.cleanupCache()
            _state.value = UpdateState.Failed(UpdateConstants.MSG_ACTIVATE_FAILED)
            return
        }

        // 5. 清理旧版本目录（仅保留当前激活版本）
        cleanupOldVersions(release.version)

        // 6. 清理下载缓存
        downloadService.cleanupCache()

        _state.value = UpdateState.Success(release.version)
    }

    // 一键检查并安装（便捷入口，供 UI 直接调用）
    suspend fun checkAndInstall() {
        val release = checkUpdate() ?: return
        downloadAndInstall(release)
    }

    // 重置状态为 Idle（UI 退出或用户确认后调用）
    fun resetState() {
        _state.value = UpdateState.Idle
    }

    // 清理旧版本目录，保留当前激活版本
    private fun cleanupOldVersions(keepVersion: String) {
        val remoteBase = File(context.filesDir, UpdateConstants.REMOTE_BASE_DIR)
        remoteBase.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name != keepVersion) {
                dir.deleteRecursively()
            }
        }
    }

    private object UpdateConstants {
        const val REMOTE_BASE_DIR = "lawnicons_remote"

        // GitHub 加速代理前缀
        const val PROXY_PREFIX = "https://ghfast.top/"
        const val MSG_FETCH_FAILED = "无法获取云端版本信息"
        const val MSG_DOWNLOAD_FAILED = "下载失败"
        const val MSG_VERIFY_FAILED = "文件校验失败"
        const val MSG_EXTRACT_FAILED = "解压失败"
        const val MSG_ACTIVATE_FAILED = "激活失败"
    }
}
