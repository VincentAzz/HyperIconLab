package com.capybara.hypericonlab.core.designsystem.blur.miuix

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported


/**
 * Remember a LayerBackdrop for Material 3.
 */
@Composable
fun rememberMaterial3BlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Determine the app bar background color for Material 3.
 */
@Composable
fun LayerBackdrop?.getMaterial3AppBarColor(): Color =
    this?.let { Color.Transparent } ?: MaterialTheme.colorScheme.surfaceContainer

/**
 * Apply a standard glassmorphism blur effect using Material 3 color schemes.
 */
@Composable
fun Modifier.material3BlurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape
): Modifier {
    if (!enabled || backdrop == null) return this
    val blendColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor)
                )
            )
        )
    )
}

/**
 * Apply a liquid glass effect (refraction + vibrancy + edge highlight) using the given backdrop.
 * Falls back to [material3BlurEffect] when runtime shaders are unsupported (below API 33).
 *
 * @param backdrop The layer backdrop to sample and refract.
 * @param shape A rounded rectangle shape supported by the lens, including the design system's
 *  smoother rounded rectangle and Compose corner-based shapes.
 * @param cornerRadius Radius used to scale the lens refraction depth; typically matches the shape.
 * @param blurRadius Gaussian blur radius applied to the sampled backdrop; larger values increase
 *  the blur strength (and readability of the surface). Defaults to 24.dp.
 */
@Composable
fun Modifier.liquidGlassEffect(
    backdrop: LayerBackdrop,
    shape: Shape,
    cornerRadius: Dp,
    blurRadius: Dp = 24.dp
): Modifier {
    if (!isRuntimeShaderSupported()) {
        return material3BlurEffect(backdrop = backdrop, shape = shape)
    }
    val containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    val highlight = remember { LiquidGlassDefaults.bottomSheetHighlight }
    return this.then(
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                val blurPx = blurRadius.toPx()
                vibrancy()
                blur(blurPx, blurPx)
                lens(
                    refractionHeight = cornerRadius.toPx(),
                    refractionAmount = cornerRadius.toPx()
                )
            },
            highlight = { highlight.copy(alpha = 0.75f) },
            onDrawSurface = { drawRect(containerColor) }
        )
    )
}

/**
 * Default highlight configurations for liquid glass surfaces.
 */
object LiquidGlassDefaults {
    val bottomSheetHighlight: Highlight = Highlight(
        width = 1.dp,
        alpha = 1f,
        style = BloomStroke(
            color = Color.White.copy(alpha = 0.12f),
            innerBlurRadius = 2.0.dp,
            primaryLight = LightSource(
                position = LightPosition(0.5f, -0.3f, -0.05f),
                color = Color.White,
                intensity = 1f,
            ),
            secondaryLight = LightSource(
                position = LightPosition(0.5f, 0.8f, -0.5f),
                color = Color.White,
                intensity = 0.4f,
            ),
            dualPeak = true,
        ),
    )
}
