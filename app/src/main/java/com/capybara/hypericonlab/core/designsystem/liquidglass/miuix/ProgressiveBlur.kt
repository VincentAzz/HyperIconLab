package com.capybara.hypericonlab.core.designsystem.liquidglass.miuix

// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).
// Alpha-masked progressive blur: uniform blur + Y-direction alpha gradient (iOS 26 style).

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported

/**
 * 在 [BackdropEffectScope] 上叠加一层 alpha-masked 渐进式模糊。
 *
 * 必须先调用 [blur]，本函数负责把整面均匀模糊的结果按 Y 方向做 alpha 渐变，
 * 顶部不透明、底部透明（iOS 26 风格）。
 *
 * @param tintColor      渐变末端叠加的色调（一般取 surfaceContainer）
 * @param tintIntensity  色调权重，0 = 纯模糊渐变，1 = 完全色调
 * @param fadeStartRatio 渐变起点 Y 比例（0~1，相对高度，不透明端）
 * @param fadeEndRatio   渐变终点 Y 比例（0~1，相对高度，透明端）
 */
fun BackdropEffectScope.progressiveBlur(
    tintColor: Color,
    tintIntensity: Float = 0.5f,
    fadeStartRatio: Float = 1.0f,
    fadeEndRatio: Float = 0.5f,
) {
    if (!isRuntimeShaderSupported()) return

    val sf = downscaleFactor.coerceAtLeast(1).toFloat()
    val w = size.width / sf
    val h = size.height / sf
    val fadeStartY = h * fadeStartRatio
    val fadeEndY = h * fadeEndRatio

    runtimeShaderEffect(
        key = "HyperIconLabProgressiveBlur",
        shaderString = PROGRESSIVE_BLUR_SHADER,
        uniformShaderName = "content",
    ) {
        setFloatUniform("size", w, h)
        setFloatUniform("fadeStartY", fadeStartY)
        setFloatUniform("fadeEndY", fadeEndY)
        setColorUniform("tint", tintColor)
        setFloatUniform("tintIntensity", tintIntensity)
    }
}

private const val PROGRESSIVE_BLUR_SHADER = """
uniform shader content;
uniform float2 size;
uniform float fadeStartY;
uniform float fadeEndY;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
    float blurAlpha = smoothstep(fadeStartY, fadeEndY, coord.y);
    float tintAlpha = smoothstep(fadeStartY, fadeEndY, coord.y);
    return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}
"""

/**
 * 渐进式模糊 Modifier（顶栏 / 标题栏使用）。
 *
 * 行为：
 * - API 33+：drawBackdrop + blur + progressiveBlur AGSL
 * - API < 33 或不支持 RuntimeShader：降级为 [material3BlurEffect]
 * - backdrop == null 或 !enabled：返回 this（与 material3BlurEffect 一致）
 *
 * @param blurRadius     均匀模糊半径，kyant 原版默认 4.dp
 * @param tintColor      渐变末端叠加的色调
 * @param tintIntensity  色调权重，0 = 纯模糊渐变，1 = 完全色调
 * @param fadeStartRatio 渐变起点 Y 比例（不透明端，0~1）
 * @param fadeEndRatio   渐变终点 Y 比例（透明端，0~1）
 */
@Composable
fun Modifier.progressiveBlurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Dp = 5.dp,
    tintColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    tintIntensity: Float = 0.5f,
    fadeStartRatio: Float = 1.0f,
    fadeEndRatio: Float = 0.7f,
    shape: Shape = RectangleShape,
): Modifier {
    if (!enabled || backdrop == null) return this
    if (!isRuntimeShaderSupported()) {
        return material3BlurEffect(backdrop = backdrop, shape = shape)
    }
    return this.then(
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                val blurPx = blurRadius.toPx()
                blur(blurPx, blurPx)
                progressiveBlur(
                    tintColor = tintColor,
                    tintIntensity = tintIntensity,
                    fadeStartRatio = fadeStartRatio,
                    fadeEndRatio = fadeEndRatio,
                )
            },
            highlight = null,
        )
    )
}

// 顶栏模糊
@Composable
fun Modifier.appBarBlurEffect(
    backdrop: LayerBackdrop?,
    useProgressiveBlur: Boolean = false,
    shape: Shape = RectangleShape,
): Modifier {
    return if (useProgressiveBlur) {
        progressiveBlurEffect(backdrop = backdrop, shape = shape)
    } else {
        material3BlurEffect(backdrop = backdrop, shape = shape)
    }
}
