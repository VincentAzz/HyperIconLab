package com.capybara.hypericonlab.modules.render.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.createBitmap
import com.capybara.hypericonlab.core.designsystem.shape.ContinuousCurvatureRoundedRectangleCornerBuilder

object CustomMaskGenerator {

    const val MASK_SIZE = 512
    private const val CUSTOM_PREFIX = "custom:"

    // 自定义 mask 编码格式：custom:<cornerRadius>:<smoothCorner>
    // cornerRadius: 0~0.5，相对于高度的比例
    // smoothCorner: 1=平滑圆角(kyant), 0=普通圆角
    fun encode(cornerRadius: Float, smoothCorner: Boolean): String =
        "$CUSTOM_PREFIX${cornerRadius}:${if (smoothCorner) 1 else 0}"

    fun isCustomMask(name: String): Boolean = name.startsWith(CUSTOM_PREFIX)

    fun parseCustomMask(name: String): Pair<Float, Boolean>? {
        if (!name.startsWith(CUSTOM_PREFIX)) return null
        val parts = name.removePrefix(CUSTOM_PREFIX).split(":")
        if (parts.size != 2) return null
        return try {
            val cornerRadius = parts[0].toFloat().coerceIn(0f, 0.5f)
            val smooth = parts[1].toInt() == 1
            cornerRadius to smooth
        } catch (_: Exception) {
            null
        }
    }

    fun generate(cornerRadius: Float, smoothCorner: Boolean): Bitmap {
        val bitmap = createBitmap(MASK_SIZE, MASK_SIZE)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val radiusPx = (MASK_SIZE * cornerRadius).coerceIn(0f, MASK_SIZE / 2f)
        val path = if (smoothCorner && radiusPx > 0f && radiusPx < MASK_SIZE / 2f) {
            buildSmoothCornerPath(MASK_SIZE.toFloat(), MASK_SIZE.toFloat(), radiusPx)
        } else if (radiusPx > 0f) {
            // 普通圆角或半径达到最大值（圆形）
            Path().apply {
                addRoundRect(
                    0f, 0f, MASK_SIZE.toFloat(), MASK_SIZE.toFloat(),
                    radiusPx, radiusPx,
                    Path.Direction.CW
                )
            }
        } else {
            Path().apply {
                addRect(
                    0f,
                    0f,
                    MASK_SIZE.toFloat(),
                    MASK_SIZE.toFloat(),
                    Path.Direction.CW
                )
            }
        }

        canvas.drawPath(path, paint)
        return bitmap
    }

    // 使用 kyant 连续曲率构造平滑圆角路径（移植自 RoundedRectangleOutline 的 Compose Path 实现）
    private fun buildSmoothCornerPath(width: Float, height: Float, radius: Float): Path {
        val path = Path()
        val cornerBuilder = ContinuousCurvatureRoundedRectangleCornerBuilder.Default
        val w = width.toDouble()
        val h = height.toDouble()
        val r = radius.toDouble()

        val tW = ((width * 0.5 - r) / r).coerceIn(0.0, 1.0)
        val tH = ((height * 0.5 - r) / r).coerceIn(0.0, 1.0)
        val p = cornerBuilder.getCornerBezierPoints(tW, tH)
        if (p.size < 20) return path.apply {
            addRoundRect(0f, 0f, width, height, radius, radius, Path.Direction.CW)
        }

        path.apply {
            var x = w - r
            var y = 0.0
            moveTo((x + p[0] * r).toFloat(), (y + p[1] * r).toFloat())
            cubicTo(
                (x + p[2] * r).toFloat(), (y + p[3] * r).toFloat(),
                (x + p[4] * r).toFloat(), (y + p[5] * r).toFloat(),
                (x + p[6] * r).toFloat(), (y + p[7] * r).toFloat()
            )
            cubicTo(
                (x + p[8] * r).toFloat(), (y + p[9] * r).toFloat(),
                (x + p[10] * r).toFloat(), (y + p[11] * r).toFloat(),
                (x + p[12] * r).toFloat(), (y + p[13] * r).toFloat()
            )
            cubicTo(
                (x + p[14] * r).toFloat(), (y + p[15] * r).toFloat(),
                (x + p[16] * r).toFloat(), (y + p[17] * r).toFloat(),
                (x + p[18] * r).toFloat(), (y + p[19] * r).toFloat()
            )

            x = w - r
            y = h
            lineTo((x + p[18] * r).toFloat(), (y - p[19] * r).toFloat())
            cubicTo(
                (x + p[16] * r).toFloat(), (y - p[17] * r).toFloat(),
                (x + p[14] * r).toFloat(), (y - p[15] * r).toFloat(),
                (x + p[12] * r).toFloat(), (y - p[13] * r).toFloat()
            )
            cubicTo(
                (x + p[10] * r).toFloat(), (y - p[11] * r).toFloat(),
                (x + p[8] * r).toFloat(), (y - p[9] * r).toFloat(),
                (x + p[6] * r).toFloat(), (y - p[7] * r).toFloat()
            )
            cubicTo(
                (x + p[4] * r).toFloat(), (y - p[5] * r).toFloat(),
                (x + p[2] * r).toFloat(), (y - p[3] * r).toFloat(),
                (x + p[0] * r).toFloat(), (y - p[1] * r).toFloat()
            )

            x = r
            y = h
            lineTo((x - p[0] * r).toFloat(), (y - p[1] * r).toFloat())
            cubicTo(
                (x - p[2] * r).toFloat(), (y - p[3] * r).toFloat(),
                (x - p[4] * r).toFloat(), (y - p[5] * r).toFloat(),
                (x - p[6] * r).toFloat(), (y - p[7] * r).toFloat()
            )
            cubicTo(
                (x - p[8] * r).toFloat(), (y - p[9] * r).toFloat(),
                (x - p[10] * r).toFloat(), (y - p[11] * r).toFloat(),
                (x - p[12] * r).toFloat(), (y - p[13] * r).toFloat()
            )
            cubicTo(
                (x - p[14] * r).toFloat(), (y - p[15] * r).toFloat(),
                (x - p[16] * r).toFloat(), (y - p[17] * r).toFloat(),
                (x - p[18] * r).toFloat(), (y - p[19] * r).toFloat()
            )

            x = r
            y = 0.0
            lineTo((x - p[18] * r).toFloat(), (y + p[19] * r).toFloat())
            cubicTo(
                (x - p[16] * r).toFloat(), (y + p[17] * r).toFloat(),
                (x - p[14] * r).toFloat(), (y + p[15] * r).toFloat(),
                (x - p[12] * r).toFloat(), (y + p[13] * r).toFloat()
            )
            cubicTo(
                (x - p[10] * r).toFloat(), (y + p[11] * r).toFloat(),
                (x - p[8] * r).toFloat(), (y + p[9] * r).toFloat(),
                (x - p[6] * r).toFloat(), (y + p[7] * r).toFloat()
            )
            cubicTo(
                (x - p[4] * r).toFloat(), (y + p[5] * r).toFloat(),
                (x - p[2] * r).toFloat(), (y + p[3] * r).toFloat(),
                (x - p[0] * r).toFloat(), (y + p[1] * r).toFloat()
            )

            close()
        }
        return path
    }
}
