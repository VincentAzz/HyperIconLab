package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.symbol.done
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.currentPreferredCardCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.RawColor
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ColorSwatchPreview
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private object ThemeColorSheetLayout {
    val DefaultHorizontalPadding = 16.dp
    val LargeHorizontalPadding = 8.dp
    val ContentTopPadding = 8.dp
    val ContentBottomPadding = 16.dp
    val ContentLargeBottomPadding = 8.dp
    val ContentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    val ColorMinimumWidth = 88.dp
    val ColorSpacing = 8.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeColorSheet(
    availableColors: List<RawColor>,
    selectedColor: Color,
    currentStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false
) {
    var draftColor by remember { mutableStateOf(selectedColor) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun closeSheet(confirm: Boolean) {
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                if (confirm) onConfirm(draftColor) else onDismiss()
            }
        }
    }

    FloatingBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        horizontalPadding = 8.dp,
        bottomPadding = 8.dp,
        cornerRadius = ExtraLargeRadius,
        fillMaxHeight = true,
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass
    ) {
        CenterAlignedTopAppBar(
            title = { SheetTitle("主题颜色") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            navigationIcon = {
                Surface(
                    onClick = { closeSheet(confirm = false) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppMaterialSymbols.close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            actions = {
                Surface(
                    onClick = { closeSheet(confirm = true) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppMaterialSymbols.done,
                            contentDescription = stringResource(R.string.confirm),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        )

        val isLargeCorner = currentPreferredCardCornerRadius() > CornerRadius
        val horizontalPadding = if (isLargeCorner) {
            ThemeColorSheetLayout.LargeHorizontalPadding
        } else {
            ThemeColorSheetLayout.DefaultHorizontalPadding
        }
        val bottomPadding = if (isLargeCorner) {
            ThemeColorSheetLayout.ContentLargeBottomPadding
        } else {
            ThemeColorSheetLayout.ContentBottomPadding
        }
        SegmentedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = ThemeColorSheetLayout.ContentTopPadding,
                    bottom = bottomPadding
                ),
            contentPadding = ThemeColorSheetLayout.ContentPadding,
            containerColorAlpha = 0.8f
        ) {
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    val columns = (maxWidth / ThemeColorSheetLayout.ColorMinimumWidth)
                        .toInt()
                        .coerceAtLeast(1)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ThemeColorSheetLayout.ColorSpacing)
                    ) {
                        availableColors.chunked(columns).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                rowItems.forEach { rawColor ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ColorSwatchPreview(
                                            rawColor = rawColor,
                                            currentStyle = currentStyle,
                                            colorSpec = colorSpec,
                                            textStyle = MaterialTheme.typography.labelMedium,
                                            textColor = MaterialTheme.colorScheme.onSurface,
                                            isSelected = draftColor == rawColor.color,
                                            onClick = { draftColor = rawColor.color }
                                        )
                                    }
                                }
                                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
