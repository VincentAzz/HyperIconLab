package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.capybara.hypericonlab.core.designsystem.blur.LiquidGlassEngine
import com.capybara.hypericonlab.core.designsystem.config.FloatingBottomBarCompactHeight
import com.capybara.hypericonlab.core.designsystem.config.FloatingBottomBarCompactIndicatorPadding
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.backdrops.LayerBackdrop as KyantLayerBackdrop

@Composable
fun FloatingBottomBarCompact(
    modifier: Modifier = Modifier,
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    m3Backdrop: LayerBackdrop? = null,
    tabsCount: Int,
    isBlurEnabled: Boolean = true,
    isStandardBlurEnabled: Boolean = false,
    engine: LiquidGlassEngine = LiquidGlassEngine.KYANT,
    kyantBackdrop: KyantLayerBackdrop? = null,
    colors: FloatingBottomBarColors = FloatingBottomBarDefaults.colors(),
    barHeight: Dp = FloatingBottomBarCompactHeight,
    indicatorPadding: Dp = FloatingBottomBarCompactIndicatorPadding,
    content: @Composable RowScope.() -> Unit
) {
    if (engine == LiquidGlassEngine.KYANT && isBlurEnabled && kyantBackdrop != null) {
        FloatingBottomBarCompactKyant(
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            backdrop = kyantBackdrop,
            m3Backdrop = m3Backdrop,
            tabsCount = tabsCount,
            isBlurEnabled = true,
            isStandardBlurEnabled = false,
            colors = colors,
            barHeight = barHeight,
            indicatorPadding = indicatorPadding,
            modifier = modifier,
            content = content
        )
    } else {
        FloatingBottomBarCompactMiuix(
            modifier = modifier,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            backdrop = backdrop,
            m3Backdrop = m3Backdrop,
            tabsCount = tabsCount,
            isBlurEnabled = isBlurEnabled,
            isStandardBlurEnabled = isStandardBlurEnabled,
            colors = colors,
            barHeight = barHeight,
            indicatorPadding = indicatorPadding,
            content = content
        )
    }
}
