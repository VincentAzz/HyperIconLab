package com.capybara.hypericonlab.modules.icon.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.io.File

/**
 * 构建产物导出器：将工件与预览图写入公共 Documents 目录。
 *
 * - Android 10+（API 29+）：使用 [MediaStore.Files] API，按 [RELATIVE_PATH] 写入
 *   `Documents/HyperIconLabArtifacts/<taskId>/`，无需运行时存储权限。
 * - Android 9 及以下（API 26-28）：直接写 [Environment.getExternalStoragePublicDirectory]
 *   下的 DOCUMENTS 子目录，需 [android.Manifest.permission.WRITE_EXTERNAL_STORAGE] 运行时授权。
 *
 * 导出目录结构（与文档第 6.1 节一致）：
 * ```
 * Documents/HyperIconLabArtifacts/<taskId>/
 * ├── icons.zip              # 工件（按 ProductType.ext 命名）
 * ├── preview_store.png      # 8 图标预览（1080×640）
 * └── preview_full.png       # 全屏预览（1080×2400）
 * ```
 */
class BuildArtifactWriter(private val context: Context) {

    /**
     * 导出工件与预览图到公共 Documents 目录。
     *
     * @param taskId 任务 id，作为子目录名
     * @param artifactFile 临时工件文件（位于 filesDir/build_temp/）
     * @param storePreview 8 图标预览图
     * @param mainPreview 全屏预览图
     * @param artifactName 工件文件名（含扩展名，如 "icons.zip"）
     * @return 导出目录路径字符串（如 "Documents/HyperIconLabArtifacts/<taskId>"），失败时返回 null
     */
    fun export(
        taskId: String,
        artifactFile: File,
        storePreview: Bitmap,
        mainPreview: Bitmap,
        artifactName: String = ExportConfig.DEFAULT_ARTIFACT_NAME
    ): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportViaMediaStore(taskId, artifactFile, storePreview, mainPreview, artifactName)
        } else {
            exportViaDirectFile(taskId, artifactFile, storePreview, mainPreview, artifactName)
        }
    }

    // API 29+：通过 MediaStore 写入，无需运行时存储权限
    private fun exportViaMediaStore(
        taskId: String,
        artifactFile: File,
        storePreview: Bitmap,
        mainPreview: Bitmap,
        artifactName: String
    ): String? {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val relativeBase = "${ExportConfig.PUBLIC_ROOT_DIR}/$taskId/"

        // 工件文件
        if (!insertFile(
                collection = collection,
                displayName = artifactName,
                mimeType = ExportConfig.ZIP_MIME_TYPE,
                relativePath = relativeBase,
                sourceFile = artifactFile
            )
        ) {
            return null
        }

        // 8 图标预览图
        insertBitmap(
            collection = collection,
            displayName = ExportConfig.STORE_PREVIEW_NAME,
            mimeType = ExportConfig.PNG_MIME_TYPE,
            relativePath = relativeBase,
            bitmap = storePreview
        )

        // 全屏预览图
        insertBitmap(
            collection = collection,
            displayName = ExportConfig.FULL_PREVIEW_NAME,
            mimeType = ExportConfig.PNG_MIME_TYPE,
            relativePath = relativeBase,
            bitmap = mainPreview
        )

        return "${ExportConfig.PUBLIC_ROOT_DIR}/$taskId"
    }

    // API 26-28：直接写文件路径，需 WRITE_EXTERNAL_STORAGE 权限
    private fun exportViaDirectFile(
        taskId: String,
        artifactFile: File,
        storePreview: Bitmap,
        mainPreview: Bitmap,
        artifactName: String
    ): String? {
        if (!hasWriteStoragePermission()) {
            Timber.tag(TAG)
                .e("WRITE_EXTERNAL_STORAGE not granted, cannot export to public Documents")
            return null
        }
        val documentsBase =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(documentsBase, "${ExportConfig.APP_SUBDIR}/$taskId")
        dir.mkdirs()

        try {
            // 工件文件
            File(dir, artifactName).outputStream().use { out ->
                artifactFile.inputStream().use { it.copyTo(out) }
            }
            // 8 图标预览图
            File(dir, ExportConfig.STORE_PREVIEW_NAME).outputStream().use { out ->
                storePreview.compress(Bitmap.CompressFormat.PNG, ExportConfig.IMAGE_QUALITY, out)
            }
            // 全屏预览图
            File(dir, ExportConfig.FULL_PREVIEW_NAME).outputStream().use { out ->
                mainPreview.compress(Bitmap.CompressFormat.PNG, ExportConfig.IMAGE_QUALITY, out)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to export via direct file for $taskId")
            return null
        }

        return "${ExportConfig.PUBLIC_ROOT_DIR}/$taskId"
    }

    // 向 MediaStore 插入普通文件并写入内容，成功返回 true
    private fun insertFile(
        collection: Uri,
        displayName: String,
        mimeType: String,
        relativePath: String,
        sourceFile: File
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
        }
        return try {
            val uri = context.contentResolver.insert(collection, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            } ?: return false
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to insert file $displayName")
            false
        }
    }

    // 向 MediaStore 插入位图并写入内容，成功返回 true
    private fun insertBitmap(
        collection: Uri,
        displayName: String,
        mimeType: String,
        relativePath: String,
        bitmap: Bitmap
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
        }
        return try {
            val uri = context.contentResolver.insert(collection, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, ExportConfig.IMAGE_QUALITY, out)
            } ?: return false
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to insert bitmap $displayName")
            false
        }
    }

    // 检查 WRITE_EXTERNAL_STORAGE 权限（仅 API 26-28 需要）
    private fun hasWriteStoragePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "BuildArtifactWriter"

        // 导出关键参数集中声明，便于调参
        private object ExportConfig {
            // 公共 Documents 下的应用子目录根名
            const val APP_SUBDIR = "HyperIconLabArtifacts"

            // 完整公共根目录路径（用于返回值展示）
            const val PUBLIC_ROOT_DIR = "Documents/$APP_SUBDIR"

            // 工件默认文件名（实际命名应由调用方按 ProductType.ext 决定）
            const val DEFAULT_ARTIFACT_NAME = "icons.zip"

            // 8 图标预览图文件名
            const val STORE_PREVIEW_NAME = "preview_store.png"

            // 全屏预览图文件名
            const val FULL_PREVIEW_NAME = "preview_full.png"

            // MIME 类型常量
            const val ZIP_MIME_TYPE = "application/zip"
            const val PNG_MIME_TYPE = "image/png"

            // 图片压缩质量（PNG 无损，保留以备未来格式切换）
            const val IMAGE_QUALITY = 100
        }
    }
}
