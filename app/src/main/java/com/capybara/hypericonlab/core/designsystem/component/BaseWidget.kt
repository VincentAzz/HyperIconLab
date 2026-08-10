package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.blur.kyant.backdrops.LocalKyantControlsBackdrop
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.theme.ChipCornerInset
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.insetCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.dynamicColorScheme
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantCapsuleShape
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape

enum class BaseWidgetIconBackgroundShape {
    CIRCLE,
    ROUNDED_RECTANGLE
}

object BaseWidgetIconBackgroundConfig {
    val Enabled = false

    val Shape = BaseWidgetIconBackgroundShape.CIRCLE
    val IconSize = 24.dp
    val BackgroundPadding = 6.dp
    val BackgroundHorizontalOffset = (-2).dp
    val RoundedRectangleCornerInset = ChipCornerInset
    val SeedPaletteStyle = PaletteStyle.TonalSpot
    val SeedColorSpec = ThemeColorSpec.SPEC_2021
}

@Immutable
private data class BaseWidgetIconBackgroundColors(
    val containerColor: Color,
    val contentColor: Color
)

@Composable
private fun rememberBaseWidgetIconBackgroundColors(
    seedColor: Color?
): BaseWidgetIconBackgroundColors {
    if (seedColor == null || seedColor == Color.Unspecified) {
        return BaseWidgetIconBackgroundColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    val isDark = AppTheme.isDark
    val seededColorScheme = remember(
        seedColor,
        isDark,
        BaseWidgetIconBackgroundConfig.SeedPaletteStyle,
        BaseWidgetIconBackgroundConfig.SeedColorSpec
    ) {
        dynamicColorScheme(
            keyColor = seedColor,
            isDark = isDark,
            style = BaseWidgetIconBackgroundConfig.SeedPaletteStyle,
            colorSpec = BaseWidgetIconBackgroundConfig.SeedColorSpec
        )
    }

    return BaseWidgetIconBackgroundColors(
        containerColor = seededColorScheme.secondaryContainer,
        contentColor = seededColorScheme.onSecondaryContainer
    )
}

val LocalSegmentedItemShape = compositionLocalOf<Shape> { RoundedCornerShape(CornerRadius) }

val LocalSegmentedContainerColorAlpha = compositionLocalOf<Float> { 1f }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BaseWidget(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    iconPlaceholder: Boolean = true,
    iconBackgroundEnabled: Boolean = BaseWidgetIconBackgroundConfig.Enabled,
    iconBackgroundSeedColor: Color? = null,
    title: String,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    description: String? = null,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    descriptionColor: Color? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    clickHaptic: HapticFeedbackType? = HapticFeedbackType.VirtualKey,
    foreContent: @Composable BoxScope.() -> Unit = {},
    trailingContent: @Composable BoxScope.(interactionSource: MutableInteractionSource) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f

    val interactionSource = remember { MutableInteractionSource() }

    val density = LocalDensity.current
    val dynamicInternalPadding = (4 * density.fontScale).dp

    val baseShape = LocalSegmentedItemShape.current

    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }.copy(alpha = LocalSegmentedContainerColorAlpha.current)

    val baseContentColor = if (selected) {
        MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val iconBackgroundColors = rememberBaseWidgetIconBackgroundColors(
        iconBackgroundSeedColor.takeIf { iconBackgroundEnabled }
    )
    val resolvedIconColor = iconColor
        ?: when {
            iconBackgroundEnabled -> iconBackgroundColors.contentColor
            selected -> baseContentColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    val finalDescriptionColor = when {
        isError -> MaterialTheme.colorScheme.error
        descriptionColor != null -> descriptionColor
        else -> baseContentColor.copy(alpha = 0.7f)
    }

    val colors = ListItemDefaults.colors(
        containerColor = backgroundColor,
        contentColor = baseContentColor,
        leadingContentColor = resolvedIconColor,
        trailingContentColor = resolvedIconColor,
        supportingContentColor = finalDescriptionColor,

        selectedContainerColor = backgroundColor,
        selectedContentColor = baseContentColor,
        selectedLeadingContentColor = resolvedIconColor,
        selectedTrailingContentColor = resolvedIconColor,
        selectedSupportingContentColor = finalDescriptionColor,

        disabledContainerColor = backgroundColor,
        disabledContentColor = baseContentColor,
        disabledLeadingContentColor = resolvedIconColor,
        disabledTrailingContentColor = resolvedIconColor,
        disabledSupportingContentColor = finalDescriptionColor
    )

    val shapes = ListItemDefaults.shapes(
        shape = baseShape,
        pressedShape = baseShape,
        selectedShape = baseShape,
        focusedShape = baseShape,
        hoveredShape = baseShape
    )

    val itemModifier = modifier.fillMaxWidth()
    val hasLeadingContent = icon != null || iconPlaceholder
    val leadingContentReporter = LocalSegmentedLeadingContentReporter.current
    val iconSize = BaseWidgetIconBackgroundConfig.IconSize
    val iconBackgroundPadding = BaseWidgetIconBackgroundConfig.BackgroundPadding.coerceAtLeast(0.dp)
    val iconContainerSize = if (iconBackgroundEnabled) {
        iconSize + iconBackgroundPadding * 2
    } else {
        iconSize
    }
    val iconBackgroundCornerRadius = insetCornerRadius(
        currentSegmentedColumnOuterCornerRadius(),
        BaseWidgetIconBackgroundConfig.RoundedRectangleCornerInset
    )
    val iconBackgroundShape = when (BaseWidgetIconBackgroundConfig.Shape) {
        BaseWidgetIconBackgroundShape.CIRCLE -> CircleShape
        BaseWidgetIconBackgroundShape.ROUNDED_RECTANGLE -> rememberKyantRoundedRectangleShape(
            iconBackgroundCornerRadius
        )
    }

    if (leadingContentReporter != null) {
        SideEffect {
            leadingContentReporter(iconContainerSize.takeIf { hasLeadingContent })
        }
    }

    val leadingContent: (@Composable () -> Unit)? =
        if (hasLeadingContent) {
            {
                val leadingModifier = Modifier
                    .size(iconContainerSize)
                    .offset(
                        x = if (iconBackgroundEnabled && icon != null) {
                            BaseWidgetIconBackgroundConfig.BackgroundHorizontalOffset
                        } else {
                            0.dp
                        }
                    )
                    .alpha(alpha)
                    .let { baseModifier ->
                        if (iconBackgroundEnabled && icon != null) {
                            baseModifier.background(
                                color = iconBackgroundColors.containerColor,
                                shape = iconBackgroundShape
                            )
                        } else {
                            baseModifier
                        }
                    }

                Box(
                    modifier = leadingModifier,
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            imageVector = icon,
                            contentDescription = null,
                            tint = resolvedIconColor
                        )
                    } else {
                        Spacer(modifier = Modifier.size(iconSize))
                    }
                }
            }
        } else {
            null
        }

    val supportingContent: (@Composable () -> Unit)? =
        description?.let { text ->
            {
                Text(
                    text = text,
                    style = descriptionStyle,
                    modifier = Modifier
                        .alpha(alpha)
                        .padding(bottom = dynamicInternalPadding)
                )
            }
        }

    val trailing: @Composable () -> Unit = {
        Box(
            modifier = Modifier.alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            trailingContent(interactionSource)
        }
    }

    val headline: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .alpha(alpha)
                .padding(
                    top = dynamicInternalPadding,
                    bottom = if (description == null) dynamicInternalPadding else 0.dp
                )
        ) {
            Text(
                text = title,
                style = titleStyle
            )

            foreContent()
        }
    }

    ListItem(
        selected = selected,
        modifier = itemModifier,
        onClick = {
            if (onClick != null) {
                clickHaptic?.let { haptic.performHapticFeedback(it) }
                onClick()
            }
        },
        enabled = enabled && onClick != null,
        colors = colors,
        shapes = shapes,
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent = trailing,
        interactionSource = interactionSource,
        content = headline
    )
}

@Composable
fun SwitchWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    iconPlaceholder: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    BaseWidget(
        icon = icon,
        title = title,
        description = description,
        enabled = enabled,
        iconPlaceholder = iconPlaceholder,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            val kyantBackdrop = LocalKyantControlsBackdrop.current
            if (enabled && LocalAppleStyleControls.current.useToggle && kyantBackdrop != null) {
                AppleLiquidToggleKyant(
                    selected = { checked },
                    onSelect = onCheckedChange,
                    backdrop = kyantBackdrop,
                    modifier = Modifier,
                    selectedTrackColor = MaterialTheme.colorScheme.primary
                )
            } else {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            }
        }
    )
}

@Composable
fun RadioButtonWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    iconPlaceholder: Boolean = true,
    onClick: () -> Unit
) {
    BaseWidget(
        modifier = Modifier.fillMaxWidth(),
        icon = icon,
        title = title,
        description = description,
        enabled = enabled,
        iconPlaceholder = iconPlaceholder,
        onClick = onClick,
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled
            )
        }
    )
}


@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = rememberKyantCapsuleShape(),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
