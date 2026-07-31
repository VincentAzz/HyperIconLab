package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

// 资源来源管理器：根据激活指针决定使用 assets 还是云端版本
// 激活指针存储在 filesDir/lawnicons_active.txt（单行版本号）
// 有指针且对应目录存在 → RemoteResourceProvider；否则 → AssetsResourceProvider
// 切换来源后通知所有依赖方重新读取资源
class LawniconsResourceManager(
    private val context: Context
) {
    // assets 回滚 provider（始终可用）
    private val assetsProvider = AssetsResourceProvider(context)

    // 当前激活的 provider（volatile 语义由 StateFlow 保证）
    @Volatile
    private var currentProvider: LawniconsResourceProvider = assetsProvider

    // 当前版本信息，供 UI 观察
    private val _currentVersion = MutableStateFlow(currentProvider.getVersion())
    val currentVersion: StateFlow<LawniconsVersion> = _currentVersion.asStateFlow()

    init {
        // 启动时检测激活指针，若云端版本存在则切换
        refresh()
    }

    // 获取当前激活的 provider
    fun getProvider(): LawniconsResourceProvider = currentProvider

    // 重新检测激活指针并更新 provider，返回是否发生了切换
    fun refresh(): Boolean {
        val activeVersion = readActiveVersion()
        if (activeVersion != null) {
            val remoteDir = File(
                context.filesDir,
                "${ManagerConstants.REMOTE_BASE_DIR}/$activeVersion"
            )
            if (remoteDir.exists()) {
                val newProvider = RemoteResourceProvider(context, activeVersion)
                val changed = currentProvider.getSourceType() != ResourceSource.REMOTE ||
                        (currentProvider.getVersion().version != activeVersion)
                currentProvider = newProvider
                _currentVersion.value = newProvider.getVersion()
                return changed
            }
        }
        // 云端版本不可用，回滚到 assets
        if (currentProvider.getSourceType() != ResourceSource.ASSETS) {
            currentProvider = assetsProvider
            _currentVersion.value = assetsProvider.getVersion()
            return true
        }
        return false
    }

    // 切换到 assets 出厂版本
    fun switchToAssets() {
        if (currentProvider.getSourceType() != ResourceSource.ASSETS) {
            currentProvider = assetsProvider
            _currentVersion.value = assetsProvider.getVersion()
            // 清除激活指针
            writeActiveVersion(null)
        }
    }

    // 切换到指定云端版本（需已下载解压），失败返回 false
    fun switchToRemote(version: String): Boolean {
        val remoteDir = File(
            context.filesDir,
            "${ManagerConstants.REMOTE_BASE_DIR}/$version"
        )
        if (!remoteDir.exists()) return false
        val newProvider = RemoteResourceProvider(context, version)
        currentProvider = newProvider
        _currentVersion.value = newProvider.getVersion()
        writeActiveVersion(version)
        return true
    }

    // 获取已下载的云端版本列表
    fun getDownloadedVersions(): List<String> {
        val remoteBase = File(context.filesDir, ManagerConstants.REMOTE_BASE_DIR)
        if (!remoteBase.exists()) return emptyList()
        return remoteBase.listFiles { f -> f.isDirectory }?.map { it.name } ?: emptyList()
    }

    // 获取 assets 出厂版本的版本信息（无论当前激活来源是什么）
    fun getAssetsVersionInfo(): LawniconsVersion = assetsProvider.getVersion()

    // 获取指定已下载云端版本的版本信息，目录不存在返回 null
    fun getRemoteVersionInfo(version: String): LawniconsVersion? {
        val remoteDir = File(
            context.filesDir,
            "${ManagerConstants.REMOTE_BASE_DIR}/$version"
        )
        if (!remoteDir.exists()) return null
        return RemoteResourceProvider(context, version).getVersion()
    }

    // 清除所有云端下载资源并切换回 assets 出厂版本（调试用，上线前移除）
    fun clearCloudAssets() {
        val remoteBase = File(context.filesDir, ManagerConstants.REMOTE_BASE_DIR)
        remoteBase.deleteRecursively()
        writeActiveVersion(null)
        currentProvider = assetsProvider
        _currentVersion.value = assetsProvider.getVersion()
    }

    // 读取激活版本指针，无指针或为空返回 null
    private fun readActiveVersion(): String? {
        val pointer = File(context.filesDir, ManagerConstants.ACTIVE_POINTER_FILE)
        if (!pointer.exists()) return null
        val version = pointer.readText().trim()
        return version.ifEmpty { null }
    }

    // 写入激活版本指针，version 为 null 时删除指针
    private fun writeActiveVersion(version: String?) {
        val pointer = File(context.filesDir, ManagerConstants.ACTIVE_POINTER_FILE)
        if (version == null) {
            pointer.delete()
        } else {
            pointer.writeText(version)
        }
    }

    private object ManagerConstants {
        const val REMOTE_BASE_DIR = "lawnicons_remote"
        const val ACTIVE_POINTER_FILE = "lawnicons_active.txt"
    }
}
