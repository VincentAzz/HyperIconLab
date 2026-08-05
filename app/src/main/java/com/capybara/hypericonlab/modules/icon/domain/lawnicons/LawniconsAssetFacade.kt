package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import com.capybara.hypericonlab.modules.icon.domain.repository.AssetUpdateCheckTrigger
import kotlinx.coroutines.flow.StateFlow

// Lawnicons 资源对应用层的统一入口
interface LawniconsAssetFacade {
    // 当前激活资源版本
    val currentVersion: StateFlow<LawniconsVersion>

    // 主资源更新状态
    val updateState: StateFlow<UpdateState>

    // 纯检查阶段的资产更新状态
    val assetCheckState: StateFlow<AssetUpdateCheckState>

    // 上次自动检查时间
    val lastAutomaticAssetCheckAt: StateFlow<Long?>

    // 上次手动检查时间
    val lastManualAssetCheckAt: StateFlow<Long?>

    // 图标包模板状态
    val templateState: StateFlow<IconPackTemplateState>

    // 获取当前资源提供者
    fun getProvider(): LawniconsResourceProvider

    // 重新检测激活资源
    fun refresh(): Boolean

    // 获取已下载的云端版本
    fun getDownloadedVersions(): List<String>

    // 获取内置资源版本
    fun getAssetsVersionInfo(): LawniconsVersion

    // 获取指定云端版本
    fun getRemoteVersionInfo(version: String): LawniconsVersion?

    // 切换到内置资源
    fun switchToAssets()

    // 切换到已下载的云端资源
    fun switchToRemote(version: String): Boolean

    // 清除云端资源并回滚到内置版本
    fun clearCloudAssets()

    // 检查并安装 Lawnicons 与模板
    suspend fun checkAndInstall()

    // 仅检查 Lawnicons 与 APK 模板更新，不执行下载
    suspend fun checkForAssetUpdates(
        trigger: AssetUpdateCheckTrigger = AssetUpdateCheckTrigger.MANUAL
    ): AssetUpdateCheckState

    // 标记资产更新及其缓存流程已完成
    suspend fun markAssetUpdateCompleted()

    // 判断指定类型的检查是否已结束冷却
    fun canCheckForAssetUpdates(
        trigger: AssetUpdateCheckTrigger = AssetUpdateCheckTrigger.MANUAL,
        now: Long = System.currentTimeMillis()
    ): Boolean

    // 获取指定类型检查的剩余冷却时间
    fun assetCheckCooldownRemainingMs(
        trigger: AssetUpdateCheckTrigger = AssetUpdateCheckTrigger.MANUAL,
        now: Long = System.currentTimeMillis()
    ): Long

    // 静默检查并安装 Lawnicons 与模板
    suspend fun checkAndInstallSilently()

    // 仅静默检查并安装 Lawnicons 资源
    suspend fun checkAndInstallLawniconsSilently(): Boolean

    // 重置主资源更新状态
    fun resetUpdateState()

    // 确保当前远程模板可用
    suspend fun ensureTemplateAvailable(onProgress: (Float) -> Unit = {}): Boolean

    // 校验当前远程模板目录是否已经准备完成
    fun isTemplateAvailable(): Boolean
}

// LawniconsAssetFacade 默认实现
class DefaultLawniconsAssetFacade(
    private val resourceManager: LawniconsResourceManager,
    private val updateManager: LawniconsUpdateManager,
    private val templateManager: IconPackTemplateManager
) : LawniconsAssetFacade {
    override val currentVersion: StateFlow<LawniconsVersion>
        get() = resourceManager.currentVersion

    override val updateState: StateFlow<UpdateState>
        get() = updateManager.state

    override val assetCheckState: StateFlow<AssetUpdateCheckState>
        get() = updateManager.assetCheckState

    override val lastAutomaticAssetCheckAt: StateFlow<Long?>
        get() = updateManager.lastAutomaticAssetCheckAt

    override val lastManualAssetCheckAt: StateFlow<Long?>
        get() = updateManager.lastManualAssetCheckAt

    override val templateState: StateFlow<IconPackTemplateState>
        get() = templateManager.state

    override fun getProvider(): LawniconsResourceProvider = resourceManager.getProvider()

    override fun refresh(): Boolean = resourceManager.refresh()

    override fun getDownloadedVersions(): List<String> = resourceManager.getDownloadedVersions()

    override fun getAssetsVersionInfo(): LawniconsVersion = resourceManager.getAssetsVersionInfo()

    override fun getRemoteVersionInfo(version: String): LawniconsVersion? =
        resourceManager.getRemoteVersionInfo(version)

    override fun switchToAssets() {
        resourceManager.switchToAssets()
    }

    override fun switchToRemote(version: String): Boolean = resourceManager.switchToRemote(version)

    override fun clearCloudAssets() {
        resourceManager.clearCloudAssets()
    }

    override suspend fun checkAndInstall() {
        updateManager.checkAndInstall()
    }

    override suspend fun checkForAssetUpdates(
        trigger: AssetUpdateCheckTrigger
    ): AssetUpdateCheckState = updateManager.checkForAssetUpdates(trigger)

    override suspend fun markAssetUpdateCompleted() {
        updateManager.markAssetUpdateCompleted()
    }

    override fun canCheckForAssetUpdates(
        trigger: AssetUpdateCheckTrigger,
        now: Long
    ): Boolean = updateManager.canCheckForAssetUpdates(trigger, now)

    override fun assetCheckCooldownRemainingMs(
        trigger: AssetUpdateCheckTrigger,
        now: Long
    ): Long = updateManager.assetCheckCooldownRemainingMs(trigger, now)

    override suspend fun checkAndInstallSilently() {
        updateManager.checkAndInstallSilently()
    }

    override suspend fun checkAndInstallLawniconsSilently(): Boolean =
        updateManager.checkAndInstallLawniconsSilently()

    override fun resetUpdateState() {
        updateManager.resetState()
    }

    override suspend fun ensureTemplateAvailable(onProgress: (Float) -> Unit): Boolean =
        templateManager.ensureAvailable(onProgress)

    override fun isTemplateAvailable(): Boolean = templateManager.isAvailable()
}
