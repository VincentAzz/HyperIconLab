package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.blur.LiquidGlassEngine
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.KyantGlassTuning
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.KyantGlassTuningParameter
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.component.StyleChip
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.symbol.done
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.SheetSegmentedColumnContentPadding
import com.capybara.hypericonlab.core.designsystem.theme.currentSheetRoundedLayout
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private object LiquidGlassSheetLayout {
    val HeaderButtonSize = 40.dp
    val HeaderIconSize = 24.dp
    val HeaderHorizontalPadding = 12.dp
    val ChipPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    val ChipSpacing = 8.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassSettingsSheet(
    initialEngine: LiquidGlassEngine,
    initialTuning: KyantGlassTuning,
    onDismiss: () -> Unit,
    onConfirm: (LiquidGlassEngine, KyantGlassTuning) -> Unit,
    onPreviewTuning: (KyantGlassTuning) -> Unit,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false
) {
    var engine by remember { mutableStateOf(initialEngine) }
    var tuning by remember { mutableStateOf(initialTuning) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val roundedLayout = currentSheetRoundedLayout()

    fun closeSheet(confirm: Boolean) {
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                if (confirm) onConfirm(engine, tuning) else onDismiss()
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
            title = { SheetTitle("自定义液态玻璃") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            navigationIcon = {
                Surface(
                    onClick = { closeSheet(confirm = false) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier
                        .padding(start = LiquidGlassSheetLayout.HeaderHorizontalPadding)
                        .size(LiquidGlassSheetLayout.HeaderButtonSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppMaterialSymbols.close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(LiquidGlassSheetLayout.HeaderIconSize)
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
                        .padding(end = LiquidGlassSheetLayout.HeaderHorizontalPadding)
                        .size(LiquidGlassSheetLayout.HeaderButtonSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppMaterialSymbols.done,
                            contentDescription = "确认",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(LiquidGlassSheetLayout.HeaderIconSize)
                        )
                    }
                }
            }
        )

        SegmentedColumn(
            modifier = Modifier.padding(
                start = roundedLayout.cardInset,
                end = roundedLayout.cardInset,
                top = roundedLayout.cardInset,
                bottom = roundedLayout.cardInset
            ),
            contentPadding = PaddingValues(SheetSegmentedColumnContentPadding),
            containerColorAlpha = 0.8f
        ) {
            item { shape ->
                BaseItemContainer(shape = shape) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(LiquidGlassSheetLayout.ChipPadding),
                            horizontalArrangement = Arrangement.spacedBy(LiquidGlassSheetLayout.ChipSpacing)
                        ) {
                            listOf(
                                LiquidGlassEngine.KYANT,
                                LiquidGlassEngine.MIUIX
                            ).forEach { option ->
                                StyleChip(
                                    label = when (option) {
                                        LiquidGlassEngine.KYANT -> "Kyant/backdrop"
                                        LiquidGlassEngine.MIUIX -> "miuix/miuix-blur"
                                    },
                                    selected = engine == option,
                                    enabled = true,
                                    onClick = { engine = option },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        val showKyantControls = engine == LiquidGlassEngine.KYANT
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            KyantGlassTuningControls(
                                tuning = tuning,
                                enabled = showKyantControls,
                                onTuningChange = { parameter, value ->
                                    tuning = tuning.copyFor(parameter, value)
                                    onPreviewTuning(tuning)
                                },
                                onValueChangeFinished = { onPreviewTuning(tuning) },
                                onPresetSelected = {
                                    tuning = it
                                    onPreviewTuning(it)
                                },
                                modifier = Modifier
                                    .alpha(if (showKyantControls) 1f else 0f)
                                    .then(
                                        if (showKyantControls) Modifier
                                        else Modifier.clearAndSetSemantics { }
                                    )
                            )
                            if (!showKyantControls) {
                                Text(
                                    text = "当前引擎未配置自定义选项",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun KyantGlassTuning.copyFor(
    parameter: KyantGlassTuningParameter,
    value: Float
): KyantGlassTuning = when (parameter) {
    KyantGlassTuningParameter.BLUR_SCALE -> copy(blurScale = value)
    KyantGlassTuningParameter.REFRACTION_HEIGHT_SCALE -> copy(refractionHeightScale = value)
    KyantGlassTuningParameter.REFRACTION_AMOUNT_SCALE -> copy(refractionAmountScale = value)
    KyantGlassTuningParameter.CHROMATIC_ABERRATION -> copy(chromaticAberration = value)
}
