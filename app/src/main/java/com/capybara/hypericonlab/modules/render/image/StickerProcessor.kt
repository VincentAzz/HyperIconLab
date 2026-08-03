package com.capybara.hypericonlab.modules.render.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import com.capybara.hypericonlab.modules.icon.domain.model.StickerConfig
import java.io.File
import java.io.FileOutputStream

/**
 * 贴纸效果处理器。
 * 使用 OpenCV (JNI) 提取蒙版，使用 Android Canvas 进行着色和合成。
 */
object StickerProcessor {

    private var appContext: Context? = null
    private val memoryCache = LruCache<String, Bitmap>(100)

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun process(
        originalIcon: Bitmap,
        config: StickerConfig,
        iconId: String? = null,
        iconScale: Float = 1.0f
    ): Bitmap {
        val baseSize = originalIcon.width

        // 1. 获取描边像素值 (基于原始图标尺寸)
        val strokePx = (baseSize * config.strokeWidth).toInt().coerceAtLeast(1)

        // 2. 优先应用缩放 (Point 1: 整体缩放)
        // 这样生成的描边和填充会自动包裹缩放后的图标
        val scaledIcon = if (iconScale != 1.0f) {
            val scaledSize = (baseSize * iconScale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(baseSize, baseSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val offset = (baseSize - scaledSize) / 2f
            canvas.drawBitmap(
                originalIcon,
                null,
                RectF(offset, offset, offset + scaledSize, offset + scaledSize),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
            bmp
        } else {
            originalIcon
        }

        // 3. 获取蒙版 (基于缩放后的图标计算)
        val strokeMask: Bitmap
        val fillMask: Bitmap?

        // 缓存 Key 包含缩放比例和描边粗细
        if (iconId != null) {
            val strokeKey =
                "${iconId}_${strokePx}_${baseSize}_s${(iconScale * 100).toInt()}_stroke_v4"
            val fillKey = "${iconId}_${strokePx}_${baseSize}_s${(iconScale * 100).toInt()}_fill_v4"

            strokeMask = getMask(strokeKey) ?: run {
                val mask = StickerNativeProcessor.nativeGetStrokeMask(scaledIcon, strokePx)
                saveMask(strokeKey, mask)
                mask
            }

            fillMask = if (config.fillStyle != "none") {
                getMask(fillKey) ?: run {
                    val mask = StickerNativeProcessor.nativeGetHoleMask(strokeMask)
                    saveMask(fillKey, mask)
                    mask
                }
            } else null
        } else {
            strokeMask = StickerNativeProcessor.nativeGetStrokeMask(scaledIcon, strokePx)
            fillMask =
                if (config.fillStyle != "none") StickerNativeProcessor.nativeGetHoleMask(strokeMask) else null
        }

        // 4. 准备输出
        val w = strokeMask.width
        val h = strokeMask.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // A. 绘制填充层 (底层)
        if (config.fillStyle != "none" && fillMask != null) {
            val fillColorInt = try {
                Color.parseColor(config.fillColor)
            } catch (_: Exception) {
                Color.TRANSPARENT
            }
            if (config.fillStyle == "fill") {
                paint.colorFilter = PorterDuffColorFilter(fillColorInt, PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(fillMask, 0f, 0f, paint)
            } else if (config.fillStyle == "glow") {
                val glowBmp = createGlowBitmap(w, h, fillColorInt, config.glowIntensity, fillMask)
                canvas.drawBitmap(glowBmp, 0f, 0f, paint)
                glowBmp.recycle()
            }
            paint.colorFilter = null
        }

        // B. 绘制白色描边层
        paint.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(strokeMask, 0f, 0f, paint)
        paint.colorFilter = null

        // C. 绘制着色后的图标层
        // JNI 内部 Padding 是 strokeWidth * 2，所以图标应该放在 (strokeWidth, strokeWidth)
        val tintedIcon = tintBitmap(
            scaledIcon, try {
                Color.parseColor(config.lineColor)
            } catch (_: Exception) {
                Color.BLACK
            }
        )
        canvas.drawBitmap(tintedIcon, strokePx.toFloat(), strokePx.toFloat(), paint)
        tintedIcon.recycle()

        // 5. 最终缩放回 originalSize (因为蒙版有 padding)
        val finalResult = Bitmap.createScaledBitmap(result, baseSize, baseSize, true)

        // 释放临时资源
        result.recycle()
        if (scaledIcon !== originalIcon) scaledIcon.recycle()
        if (iconId == null) {
            strokeMask.recycle()
            fillMask?.recycle()
        }

        return finalResult
    }

    private fun getMask(key: String): Bitmap? {
        memoryCache.get(key)?.let { return it }
        val context = appContext ?: return null
        val cacheFile = File(getCacheDir(context), "$key.png")
        if (cacheFile.exists()) {
            val bmp = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bmp != null) {
                memoryCache.put(key, bmp)
                return bmp
            }
        }
        return null
    }

    private fun saveMask(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
        val context = appContext ?: return
        Thread {
            try {
                val file = File(getCacheDir(context), "$key.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } catch (_: Exception) {
            }
        }.start()
    }

    fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "sticker_masks")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun createGlowBitmap(
        w: Int,
        h: Int,
        color: Int,
        intensity: Float,
        mask: Bitmap
    ): Bitmap {
        val glowLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(glowLayer)
        val centerX = w / 2f
        val centerY = h / 2f
        val radius = (w / 2f) * intensity
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(color, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, radius * 2, paint)

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val resCanvas = Canvas(result)
        resCanvas.drawBitmap(glowLayer, 0f, 0f, null)
        paint.shader = null
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        resCanvas.drawBitmap(mask, 0f, 0f, paint)
        glowLayer.recycle()
        return result
    }

    private fun tintBitmap(src: Bitmap, color: Int): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
