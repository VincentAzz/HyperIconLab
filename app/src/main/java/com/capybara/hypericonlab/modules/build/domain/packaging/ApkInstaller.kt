package com.capybara.hypericonlab.modules.build.domain.packaging

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri

interface ApkInstallFacade {
    sealed interface LaunchResult {
        data object Launched : LaunchResult
        data object UnknownSourcesPermissionRequired : LaunchResult
        data class Failed(val message: String) : LaunchResult
    }

    fun canInstallUnknownSources(): Boolean

    fun openUnknownSourcesSettings(): Boolean

    fun launchInstaller(apkUri: Uri): LaunchResult
}

class ApkInstaller(private val context: Context) : ApkInstallFacade {
    override fun canInstallUnknownSources(): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    override fun openUnknownSourcesSettings(): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startActivitySafely(intent)
    }

    override fun launchInstaller(apkUri: Uri): ApkInstallFacade.LaunchResult {
        if (!canInstallUnknownSources()) {
            return ApkInstallFacade.LaunchResult.UnknownSourcesPermissionRequired
        }
        if (apkUri.scheme != ContentResolver.SCHEME_CONTENT) {
            return ApkInstallFacade.LaunchResult.Failed("APK Uri 必须是 content:// Uri")
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, InstallerConfig.APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(InstallerConfig.CLIP_DATA_LABEL, apkUri)
        }
        if (context.packageManager.resolveActivity(
                intent, PackageManager.MATCH_DEFAULT_ONLY
            ) == null
        ) {
            return ApkInstallFacade.LaunchResult.Failed("系统没有可处理 APK 安装的应用")
        }

        return if (startActivitySafely(intent)) {
            ApkInstallFacade.LaunchResult.Launched
        } else {
            ApkInstallFacade.LaunchResult.Failed("无法启动系统安装器")
        }
    }

    private fun startActivitySafely(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private object InstallerConfig {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val CLIP_DATA_LABEL = "HyperIconLab APK"
    }
}
