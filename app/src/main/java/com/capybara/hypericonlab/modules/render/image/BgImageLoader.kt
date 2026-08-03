package com.capybara.hypericonlab.modules.render.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class BgImageDir(val assetDir: String, val fileDir: String) {
    STATIC("img_static", "img_static_custom"),
    FILLING("img_filling", "img_filling_custom")
}

object BgImageLoader {

    private const val MIN_RESOLUTION = 256

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

    suspend fun saveFromUri(context: Context, uri: Uri, dir: BgImageDir): String? =
        withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                if (opts.outWidth <= 0 || opts.outHeight <= 0) return@withContext null
                if (opts.outWidth < MIN_RESOLUTION || opts.outHeight < MIN_RESOLUTION) {
                    return@withContext null
                }

                val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, 512)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: return@withContext null

                val result = saveBitmap(context, bitmap, dir)
                bitmap.recycle()
                result
            } catch (_: Exception) {
                null
            }
        }

    fun deleteCustomFile(context: Context, ref: String) {
        if (!ref.startsWith("file:")) return
        val relativePath = ref.removePrefix("file:")
        val file = File(context.filesDir, relativePath)
        if (file.exists()) file.delete()
    }

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


    fun listCustomFiles(context: Context, dir: BgImageDir): List<String> {
        val customDir = File(context.filesDir, dir.fileDir)
        if (!customDir.exists()) return emptyList()
        return customDir.listFiles()
            ?.filter { it.isFile }
            ?.filter {
                it.name.endsWith(".png", ignoreCase = true) ||
                        it.name.endsWith(".jpg", ignoreCase = true) ||
                        it.name.endsWith(".jpeg", ignoreCase = true)
            }
            ?.sortedBy { it.name }
            ?.map { "file:${dir.fileDir}/${it.name}" }
            ?: emptyList()
    }

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
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
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
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
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
        if (src.width == targetSize && src.height == targetSize) {
            return if (src.config != Bitmap.Config.ARGB_8888 || !src.hasAlpha()) {
                val converted = src.copy(Bitmap.Config.ARGB_8888, true)
                if (converted !== src) src.recycle()
                converted.apply { setHasAlpha(true) }
            } else src
        }
        val scaled = src.scale(targetSize, targetSize)
        if (scaled !== src) src.recycle()
        scaled.setHasAlpha(true)
        return scaled
    }
}
