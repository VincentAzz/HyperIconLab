package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import android.content.Context
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

// 图标包模板按需下载管理器：模板与当前 remote 资源版本严格绑定。
class IconPackTemplateManager(
    private val context: Context,
    private val apiService: LawniconsApiService,
    private val downloadService: LawniconsDownloadService,
    private val resourceManager: LawniconsResourceManager,
    private val appSettingsRepository: AppSettingsRepository,
    private val archive: IconPackTemplateArchive
) {
    suspend fun ensureAvailable(onProgress: (Float) -> Unit = {}): Boolean {
        val currentVersion = resourceManager.currentVersion.value
        if (currentVersion.source != ResourceSource.REMOTE) return false
        require(TemplateConstants.VERSION_PATTERN.matches(currentVersion.version)) {
            "远程资源版本格式无效"
        }

        val finalDir = versionDir(currentVersion.version)
        if (validateExisting(finalDir, currentVersion)) return true

        val useProxy = appSettingsRepository.preferencesFlow.first().useDownloadProxy
        val proxyPrefix = if (useProxy) TemplateConstants.PROXY_PREFIX else ""
        val release = apiService.getRelease(currentVersion.version, proxyPrefix) ?: return false
        val templateAsset = release.templateArchive ?: return false
        require(
            currentVersion.lawniconsCommit.isBlank() ||
                    currentVersion.lawniconsCommit == release.lawniconsCommit
        ) { "当前资源与 Release commit 不一致" }

        val cacheName = "iconpack_templates_${currentVersion.version}.zip"
        val archiveFile = downloadService.download(
            url = templateAsset.url,
            expectedSize = templateAsset.sizeBytes,
            cacheFileName = cacheName,
            onProgress = onProgress
        )
        try {
            installAtomically(
                archiveFile = archiveFile,
                version = currentVersion.version,
                commit = release.lawniconsCommit
            )
        } catch (e: Exception) {
            throw LawniconsUpdateException(FailureReason.CORRUPTED, e.message, e)
        } finally {
            downloadService.cleanupCache(cacheName)
        }
        cleanupOtherVersions(currentVersion.version)
        return true
    }

    fun isAvailable(): Boolean {
        val currentVersion = resourceManager.currentVersion.value
        if (currentVersion.source != ResourceSource.REMOTE) return false
        return validateExisting(versionDir(currentVersion.version), currentVersion)
    }

    private fun validateExisting(directory: File, version: LawniconsVersion): Boolean {
        if (!directory.isDirectory || version.lawniconsCommit.isBlank()) return false
        return runCatching {
            archive.validateDirectory(directory, version.version, version.lawniconsCommit)
        }.isSuccess
    }

    private suspend fun installAtomically(archiveFile: File, version: String, commit: String) =
        withContext(Dispatchers.IO) {
            val baseDir = File(context.filesDir, TemplateConstants.TEMPLATE_BASE_DIR)
            val finalDir = File(baseDir, version)
            val stagingDir = File(baseDir, "$version${TemplateConstants.STAGING_SUFFIX}")
            val backupDir = File(baseDir, "$version${TemplateConstants.BACKUP_SUFFIX}")
            stagingDir.deleteRecursively()
            backupDir.deleteRecursively()
            baseDir.mkdirs()

            archive.extractAndValidate(archiveFile, stagingDir, version, commit)
            if (finalDir.exists() && !finalDir.renameTo(backupDir)) {
                stagingDir.deleteRecursively()
                error("无法备份旧模板目录")
            }
            if (!stagingDir.renameTo(finalDir)) {
                backupDir.renameTo(finalDir)
                stagingDir.deleteRecursively()
                error("无法原子切换模板目录")
            }
            backupDir.deleteRecursively()
        }

    private fun cleanupOtherVersions(keepVersion: String) {
        val baseDir = File(context.filesDir, TemplateConstants.TEMPLATE_BASE_DIR)
        baseDir.listFiles()?.forEach { file ->
            if (file.name != keepVersion) file.deleteRecursively()
        }
    }

    private fun versionDir(version: String) =
        File(context.filesDir, "${TemplateConstants.TEMPLATE_BASE_DIR}/$version")

    private object TemplateConstants {
        const val TEMPLATE_BASE_DIR = "lawnicons_templates"
        const val STAGING_SUFFIX = ".staging"
        const val BACKUP_SUFFIX = ".backup"
        const val PROXY_PREFIX = "https://ghfast.top/"
        val VERSION_PATTERN = Regex("^[0-9]{8}$")
    }
}
