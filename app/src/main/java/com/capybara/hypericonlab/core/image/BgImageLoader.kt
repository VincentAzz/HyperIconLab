package com.capybara.hypericonlab.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import com.capybara.hypericonlab.core.image.BgImageLoader.MIN_RESOLUTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 背景图片目录类型。
 * - assetDir: assets 中的预设目录名
 * - fileDir: filesDir 中的自选图片存储目录名
 */
enum class BgImageDir(val assetDir: String, val fileDir: String) {
    STATIC("img_static", "img_static_custom"),
    FILLING("img_filling", "img_filling_custom")
}

/**
 * 背景图片加载工具。
 *
 * 统一处理两类图片来源：
 * - 预设图片（assets 只读）：引用格式 "asset:<相对路径>"，如 "asset:img_static/01.png"
 * - 自选图片（filesDir 持久化）：引用格式 "file:<相对路径>"，如 "file:img_static_custom/1700000000_0.png"
 *
 * 所有加载均按目标尺寸降采样解码，避免大图 OOM 和性能问题。
 */
object BgImageLoader {

    /** 相册图片最小分辨率要求 */
    private const val MIN_RESOLUTION = 256

    /**
     * 从引用加载图片并缩放到指定尺寸（正方形）。
     *
     * @param ref 图片引用，如 "asset:img_static/01.png" 或 "file:img_static_custom/xx.png"
     * @param targetSize 目标边长（像素），加载后缩放到 targetSize × targetSize
     * @return 缩放后的 Bitmap，加载失败返回 null
     */
    fun loadScaled(context: Context, ref: String, targetSize: Int): Bitmap? {
        return when {
            ref.startsWith("asset:") -> loadAssetScaled(
                context,
                ref.removePrefix("asset:"),
                targetSize
            )

            ref.startsWith("file:") -> loadFileScaled(
                context,
                ref.removePrefix("file:"),
                targetSize
            )

            else -> null
        }
    }

    /**
     * 从相册 Uri 保存图片到内部存储，校验分辨率 > [MIN_RESOLUTION]×[MIN_RESOLUTION]。
     *
     * @param uri 相册 Uri（权限仅限当次会话，必须立即复制）
     * @param dir 存储目录类型
     * @return 保存成功返回引用 "file:<相对路径>"，校验失败或保存失败返回 null
     */
    suspend fun saveFromUri(context: Context, uri: Uri, dir: BgImageDir): String? =
        withContext(Dispatchers.IO) {
            try {
                // 1. 先解码尺寸校验
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                if (opts.outWidth <= 0 || opts.outHeight <= 0) return@withContext null
                if (opts.outWidth < MIN_RESOLUTION || opts.outHeight < MIN_RESOLUTION) {
                    return@withContext null
                }

                // 2. 降采样解码
                val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, 512)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: return@withContext null

                // 3. 保存到内部存储
                val result = saveBitmap(context, bitmap, dir)
                bitmap.recycle()
                result
            } catch (_: Exception) {
                null
            }
        }

    /**
     * 删除自选图片文件。
     * 仅对 "file:" 引用有效，预设图片不删除。
     */
    fun deleteCustomFile(context: Context, ref: String) {
        if (!ref.startsWith("file:")) return
        val relativePath = ref.removePrefix("file:")
        val file = File(context.filesDir, relativePath)
        if (file.exists()) file.delete()
    }

    /**
     * 枚举预设图片引用列表，按文件名字母排序。
     */
    fun listPresetAssets(context: Context, dir: BgImageDir): List<String> {
        return try {
            context.assets.list(dir.assetDir)
                ?.filter {
                    it.endsWith(".png", ignoreCase = true) || it.endsWith(
                        ".jpg",
                        ignoreCase = true
                    ) || it.endsWith(".jpeg", ignoreCase = true)
                }
                ?.sorted()
                ?.map { "asset:${dir.assetDir}/$it" }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 从引用中提取展示名称：去扩展名 + 下划线转空格。
     * 如 "asset:img_static/fluffy_round_00.png" → "fluffy round 00"
     */
    fun refToDisplayName(ref: String): String {
        val fileName = ref.substringAfterLast("/")
        val nameNoExt = fileName.substringBeforeLast(".")
        return nameNoExt.replace("_", " ")
    }


    private fun loadAssetScaled(context: Context, relativePath: String, targetSize: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(relativePath).use { BitmapFactory.decodeStream(it, null, opts) }
            if (opts.outWidth <= 0) return null
            val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, targetSize)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val raw = context.assets.open(relativePath).use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null
            scaleToSquare(raw, targetSize)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadFileScaled(context: Context, relativePath: String, targetSize: Int): Bitmap? {
        return try {
            val file = File(context.filesDir, relativePath)
            if (!file.exists()) return null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            if (opts.outWidth <= 0) return null
            val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, targetSize)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
            scaleToSquare(raw, targetSize)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, dir: BgImageDir): String? {
        return try {
            val outDir = File(context.filesDir, dir.fileDir)
            if (!outDir.exists()) outDir.mkdirs()
            val fileName =
                "${System.currentTimeMillis()}_${System.nanoTime().hashCode() and 0xFFFF}.png"
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            "file:${dir.fileDir}/$fileName"
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= targetSize && h / 2 >= targetSize) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleToSquare(src: Bitmap, targetSize: Int): Bitmap {
        if (src.width == targetSize && src.height == targetSize) return src
        val scaled = src.scale(targetSize, targetSize)
        if (scaled !== src) src.recycle()
        return scaled
    }
}
