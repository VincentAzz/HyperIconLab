package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.symbol.done
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.currentSheetRoundedLayout
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.RawColor
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ColorSwatchPreview
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private object ThemeColorSheetLayout {
    const val ColorColumns = 4
    val CardContentPadding = 8.dp
    val ColorHorizontalSpacing = 4.dp
    val ColorVerticalSpacing = 0.dp
    val SwatchVerticalPadding = 4.dp
    val SwatchLabelSpacing = 6.dp
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
    val roundedLayout = currentSheetRoundedLayout()

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
        fillMaxHeight = false,
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

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = roundedLayout.cardInset,
                    end = roundedLayout.cardInset,
                    top = roundedLayout.cardInset,
                    bottom = roundedLayout.cardInset
                ),
            shape = rememberKyantRoundedRectangleShape(roundedLayout.cardCornerRadius),
            color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ThemeColorSheetLayout.CardContentPadding),
                verticalArrangement = Arrangement.spacedBy(
                    ThemeColorSheetLayout.ColorVerticalSpacing
                )
            ) {
                availableColors.chunked(ThemeColorSheetLayout.ColorColumns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            ThemeColorSheetLayout.ColorHorizontalSpacing
                        )
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
                                    verticalPadding = ThemeColorSheetLayout.SwatchVerticalPadding,
                                    labelSpacing = ThemeColorSheetLayout.SwatchLabelSpacing,
                                    onClick = { draftColor = rawColor.color }
                                )
                            }
                        }
                        repeat(ThemeColorSheetLayout.ColorColumns - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
