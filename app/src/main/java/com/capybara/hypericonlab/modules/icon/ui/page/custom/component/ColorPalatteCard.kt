package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.color.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.color.material.RawColor
import com.capybara.hypericonlab.core.designsystem.color.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.color.material.dynamicColorScheme
import com.capybara.hypericonlab.core.designsystem.color.material.getDisplayName
import com.capybara.hypericonlab.core.designsystem.config.CornerRadius
import com.capybara.hypericonlab.core.designsystem.config.LargeCardRadius
import com.capybara.hypericonlab.core.designsystem.config.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.designsystem.symbol.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.symbol.check
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val colorSchemeCache = ConcurrentHashMap<String, ColorScheme>()

private object ColorSwatchPreviewLayout {
    val VerticalPadding = 8.dp
    val LabelSpacing = 12.dp
}

@Composable
fun ColorSwatchPreview(
    rawColor: RawColor,
    currentStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    isSelected: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    verticalPadding: Dp = ColorSwatchPreviewLayout.VerticalPadding,
    labelSpacing: Dp = ColorSwatchPreviewLayout.LabelSpacing,
    onClick: () -> Unit
) {
    val isDarkForPreview = false

    val cacheKey = remember(rawColor.color, currentStyle, colorSpec, isDarkForPreview) {
        "${rawColor.color.toArgb()}_${currentStyle.name}_${colorSpec.name}_$isDarkForPreview"
    }

    val scheme by produceState(
        initialValue = colorSchemeCache[cacheKey],
        key1 = cacheKey
    ) {
        val cachedScheme = colorSchemeCache[cacheKey]
        if (cachedScheme != null) {
            value = cachedScheme
        } else {
            withContext(Dispatchers.Default) {
                val newScheme = dynamicColorScheme(
                    keyColor = rawColor.color,
                    isDark = isDarkForPreview,
                    style = currentStyle,
                    colorSpec = colorSpec
                )
                colorSchemeCache[cacheKey] = newScheme
                value = newScheme
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(rememberKyantRoundedRectangleShape(LargeCardRadius))
            .clickable(onClick = onClick)
            .padding(vertical = verticalPadding)
    ) {
        val currentScheme = scheme
        if (currentScheme != null) {
            FullSwatchContent(
                currentScheme,
                isSelected
            )
        } else {
            FallbackSwatchContent(
                rawColor.color,
                isSelected
            )
        }

        if (rawColor.getDisplayName() !== rawColor.key) {
            Spacer(Modifier.height(labelSpacing))
            Text(
                text = rawColor.getDisplayName(),
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ColorSwatchPreviewIcon(
    rawColor: RawColor,
    currentStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    isSelected: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    onClick: () -> Unit
) {
    val isDarkForPreview = false

    val cacheKey = remember(rawColor.color, currentStyle, colorSpec, isDarkForPreview) {
        "${rawColor.color.toArgb()}_${currentStyle.name}_${colorSpec.name}_$isDarkForPreview"
    }

    val scheme by produceState(
        initialValue = colorSchemeCache[cacheKey],
        key1 = cacheKey
    ) {
        val cachedScheme = colorSchemeCache[cacheKey]
        if (cachedScheme != null) {
            value = cachedScheme
        } else {
            withContext(Dispatchers.Default) {
                val newScheme = dynamicColorScheme(
                    keyColor = rawColor.color,
                    isDark = isDarkForPreview,
                    style = currentStyle,
                    colorSpec = colorSpec
                )
                colorSchemeCache[cacheKey] = newScheme
                value = newScheme
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(rememberKyantRoundedRectangleShape(LargeCardRadius))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        val currentScheme = scheme
        if (currentScheme != null) {
            IconSwatchContent(
                currentScheme,
                isSelected
            )
        } else {
            FallbackIconSwatchContent(
                rawColor.color,
                isSelected
            )
        }

        if (rawColor.getDisplayName() !== rawColor.key) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = rawColor.getDisplayName(),
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IconSwatchContent(scheme: ColorScheme, isSelected: Boolean) {
    val foregroundForSwatch = remember(scheme) { scheme.primaryContainer.copy(alpha = 0.9f) }
    val backgroundForSwatch = remember(scheme) { scheme.primary }
    val squircleBackgroundColor = remember(scheme) { scheme.primary.copy(alpha = 0.3f) }

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                color = squircleBackgroundColor,
                shape = rememberKyantRoundedRectangleShape(CornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top half: Foreground (primaryContainer)
                drawArc(
                    color = foregroundForSwatch,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                // Bottom half: Background (primary)
                drawArc(
                    color = backgroundForSwatch,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppMaterialSymbols.check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackIconSwatchContent(baseColor: Color, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                color = baseColor.copy(alpha = 0.1f),
                shape = rememberKyantRoundedRectangleShape(CornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(baseColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppMaterialSymbols.check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullSwatchContent(scheme: ColorScheme, isSelected: Boolean) {
    val primaryForSwatch = remember(scheme) { scheme.primaryContainer.copy(alpha = 0.9f) }
    val secondaryForSwatch = remember(scheme) { scheme.secondaryContainer.copy(alpha = 0.6f) }
    val tertiaryForSwatch = remember(scheme) { scheme.tertiaryContainer.copy(alpha = 0.9f) }
    val squircleBackgroundColor = remember(scheme) { scheme.primary.copy(alpha = 0.3f) }

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                color = squircleBackgroundColor,
                shape = rememberKyantRoundedRectangleShape(CornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = primaryForSwatch,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = tertiaryForSwatch,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true
                )
                drawArc(
                    color = secondaryForSwatch,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = AppMaterialSymbols.check,
                        contentDescription = null,
                        tint = scheme.inversePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackSwatchContent(baseColor: Color, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                color = baseColor.copy(alpha = 0.1f),
                shape = rememberKyantRoundedRectangleShape(CornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(baseColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(baseColor),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = AppMaterialSymbols.check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
