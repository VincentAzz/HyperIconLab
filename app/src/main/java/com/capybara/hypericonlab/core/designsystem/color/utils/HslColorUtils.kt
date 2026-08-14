package com.capybara.hypericonlab.core.designsystem.color.utils

import androidx.core.graphics.toColorInt
import kotlin.math.abs

object HslColorUtils {

    // 自动亮度优化调参常量
    private object LuminanceAdjust {
        // 亮度判定阈值：L > 该值视为"亮色"，否则视为"暗色"
        const val LUMINANCE_THRESHOLD = 0.5f

        // 亮度偏移量：亮色减去该值调暗，暗色加上该值调亮，确保上下层有可见对比度
        const val LUMINANCE_OFFSET = 0.2f

        // 亮度边界保护：限制在 [0, 1] 范围内
        const val LUMINANCE_MIN = 0f
        const val LUMINANCE_MAX = 1f
    }

    /**
     * 基于上层背景颜色生成下层互补亮度颜色。
     *
     * 算法：
     *   1. 解析 hex 为 RGB
     *   2. RGB 转 HSL
     *   3. 若上层 L > 0.5（亮色）→ 下层 L - 0.2（调暗）
     *      若上层 L ≤ 0.5（暗色）→ 下层 L + 0.2（调亮）
     *   4. HSL 转回 RGB，输出 #AARRGGBB hex（保留原 alpha）
     *
     * @param upperHex 上层背景颜色 hex（#AARRGGBB 或 #RRGGBB）
     * @return 下层互补亮度颜色 hex（#AARRGGBB）
     */
    fun adjustLuminanceForContrast(upperHex: String): String {
        return try {
            val color = upperHex.toColorInt()
            val alpha = (color ushr 24) and 0xFF
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF

            val hsl = rgbToHsl(r, g, b)
            // 亮度互补：亮色调暗，暗色调亮
            hsl[2] = if (hsl[2] > LuminanceAdjust.LUMINANCE_THRESHOLD) {
                (hsl[2] - LuminanceAdjust.LUMINANCE_OFFSET).coerceAtLeast(LuminanceAdjust.LUMINANCE_MIN)
            } else {
                (hsl[2] + LuminanceAdjust.LUMINANCE_OFFSET).coerceAtMost(LuminanceAdjust.LUMINANCE_MAX)
            }

            val rgb = hslToRgb(hsl[0], hsl[1], hsl[2])
            String.format("#%02X%02X%02X%02X", alpha, rgb[0], rgb[1], rgb[2])
        } catch (_: Exception) {
            // 解析失败时原样返回，避免中断生成流程
            upperHex
        }
    }

    /**
     * RGB 转 HSL。
     * 返回 float 数组 [H, S, L]，H 范围 [0,360)，S/L 范围 [0,1]。
     */
    private fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        // 亮度 L
        val l = (max + min) / 2f

        // 饱和度 S
        val s = if (delta == 0f) {
            0f
        } else {
            delta / (1f - abs(2f * l - 1f))
        }

        // 色相 H
        val h = when {
            delta == 0f -> 0f
            max == rf -> 60f * (((gf - bf) / delta) % 6f)
            max == gf -> 60f * ((bf - rf) / delta + 2f)
            else -> 60f * ((rf - gf) / delta + 4f)
        }

        return floatArrayOf(if (h < 0) h + 360f else h, s, l)
    }

    /**
     * HSL 转 RGB。
     * 输入 H 范围 [0,360)，S/L 范围 [0,1]，返回 IntArray [R, G, B] 范围 [0,255]。
     */
    private fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
        if (s == 0f) {
            // 灰度色，R=G=B=L
            val gray = (l * 255f).toInt()
            return intArrayOf(gray, gray, gray)
        }

        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return intArrayOf(
            ((r + m) * 255f).toInt(),
            ((g + m) * 255f).toInt(),
            ((b + m) * 255f).toInt()
        )
    }
}