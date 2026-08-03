package com.capybara.hypericonlab.modules.build.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.capybara.hypericonlab.modules.build.domain.model.ProductType
import timber.log.Timber
import java.io.File

/**`
 * Documents/HyperIconLabArtifacts/<taskId>/
 * ├── icons.zip              # 工件（按 ProductType.ext 命名）
 * ├── preview_store.png      # 8 图标预览（1080×640）
 * └── preview_full.png       # 全屏预览（1080×2400）
 * ```
 */
class BuildArtifactWriter(private val context: Context) {

    data class ExportResult(
        val displayPath: String,
        val artifactUri: Uri?
    )

    /**
     * 导出工件与预览图到公共 Documents 目录。
     *
     * @param taskId 任务 id，作为子目录名
     * @param artifactFile 临时工件文件（位于 filesDir/build_temp/）
     * @param storePreview 8 图标预览图
     * @param mainPreview 全屏预览图
     * @param artifactName 工件文件名（含扩展名，如 "icons.zip"）
     * @return 导出结果，失败时返回 null
     */
    fun export(
        taskId: String,
        artifactFile: File,
        storePreview: Bitmap,
        mainPreview: Bitmap,
        artifactName: String = ExportConfig.DEFAULT_ARTIFACT_NAME,
        productType: ProductType = ProductType.ZIP_ICONS
    ): ExportResult? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportViaMediaStore(
                taskId,
                artifactFile,
                storePreview,
                mainPreview,
                artifactName,
                productType.mimeType
            )
        } else {
            exportViaDirectFile(
                taskId,
                artifactFile,
                storePreview,
                mainPreview,
                artifactName,
                productType
            )
        }
    }

    private fun exportViaMediaStore(
        taskId: String,
        artifactFile: File,
        storePreview: Bitmap,
        mainPreview: Bitmap,
        artifactName: String,
        artifactMimeType: String
    ): ExportResult? {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val relativeBase = "${ExportConfig.PUBLIC_ROOT_DIR}/$taskId/"

        // 工件文件：保留 MediaStore 返回的 Uri，供后续 APK 安装器使用
        val artifactUri = insertFile(
            collection = collection,
            displayName = artifactName,
            mimeType = artifactMimeType,
            relativePath = relativeBase,
            sourceFile = artifactFile
        ) ?: return null

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

        return ExportResult(
            displayPath = "${ExportConfig.PUBLIC_ROOT_DIR}/$taskId",
            artifactUri = artifactUri
        )
    }

    // API 26-28：直接写文件路径，需 WRITE_EXTERNAL_STORAGE 权限
    private fun exportViaDirectFile(
        taskId: String,
        artifactFile: File,
        storePreview: Bitmap,
        mainPreview: Bitmap,
        artifactName: String,
        productType: ProductType
    ): ExportResult? {
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

        val artifactUri = if (productType == ProductType.APK) {
            createApkShareUri(taskId, artifactFile, artifactName) ?: return null
        } else {
            null
        }
        return ExportResult(
            displayPath = "${ExportConfig.PUBLIC_ROOT_DIR}/$taskId",
            artifactUri = artifactUri
        )
    }

    // 将 APK 复制到受 FileProvider 限制的缓存目录，供安装器临时读取。
    private fun createApkShareUri(
        taskId: String,
        artifactFile: File,
        artifactName: String
    ): Uri? {
        val cacheDir = File(context.cacheDir, ExportConfig.APK_INSTALL_CACHE_DIRNAME)
        val shareFile = File(cacheDir, "${taskId}_$artifactName")
        return try {
            cacheDir.mkdirs()
            artifactFile.copyTo(shareFile, overwrite = true)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}${ExportConfig.FILE_PROVIDER_SUFFIX}",
                shareFile
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to prepare APK share Uri for $taskId")
            shareFile.delete()
            null
        }
    }

    // 向 MediaStore 插入普通文件并写入内容，成功返回 Uri
    private fun insertFile(
        collection: Uri,
        displayName: String,
        mimeType: String,
        relativePath: String,
        sourceFile: File
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
        }
        return try {
            val uri = context.contentResolver.insert(collection, values) ?: return null
            val output = context.contentResolver.openOutputStream(uri)
            if (output == null) {
                context.contentResolver.delete(uri, null, null)
                return null
            }
            output.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            }
            uri
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to insert file $displayName")
            null
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
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

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
            const val PNG_MIME_TYPE = "image/png"

            // 图片压缩质量（PNG 无损，保留以备未来格式切换）
            const val IMAGE_QUALITY = 100

            // FileProvider 允许共享的 APK 缓存目录
            const val APK_INSTALL_CACHE_DIRNAME = "apk-install"

            // FileProvider authority 后缀
            const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        }
    }
}
