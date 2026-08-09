package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.capybara.hypericonlab.core.designsystem.blur.LiquidGlassEngine
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.backdrops.LayerBackdrop as KyantLayerBackdrop

@Composable
fun FloatingBottomBar(
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
    content: @Composable RowScope.() -> Unit
) {
    if (engine == LiquidGlassEngine.KYANT && isBlurEnabled && kyantBackdrop != null) {
        FloatingBottomBarKyant(
            selectedTabIndex = selectedIndex,
            onTabSelected = onSelected,
            backdrop = kyantBackdrop,
            tabsCount = tabsCount,
            modifier = modifier,
            colors = colors,
            content = content
        )
    } else {
        FloatingBottomBarMiuix(
            modifier = modifier,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            backdrop = backdrop,
            m3Backdrop = m3Backdrop,
            tabsCount = tabsCount,
            isBlurEnabled = isBlurEnabled,
            isStandardBlurEnabled = isStandardBlurEnabled,
            colors = colors,
            content = content
        )
    }
}
