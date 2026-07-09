package com.capybara.hypericonlab.modules.settings.ui.page.settings

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRow
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowAlignment
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowWidthMode
import com.capybara.hypericonlab.core.designsystem.component.SelectionSheet
import com.capybara.hypericonlab.core.designsystem.liquidglass.appBarBlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.getMaterial3AppBarColor
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.core.designsystem.symbol.info
import com.capybara.hypericonlab.core.designsystem.symbol.inventory_2
import com.capybara.hypericonlab.core.designsystem.symbol.settings
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeSettingsAction
import com.capybara.hypericonlab.modules.settings.ui.page.settings.tabs.AboutTab
import com.capybara.hypericonlab.modules.settings.ui.page.settings.tabs.SettingsTab
import org.koin.androidx.compose.koinViewModel

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    viewModel: SettingsViewModel = koinViewModel(),
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)

    val settingTabs = listOf("设置", "资产", "关于")
    val settingIcons =
        listOf(
            AppMaterialSymbols.settings,
            AppMaterialSymbols.inventory_2,
            AppMaterialSymbols.info
        )

    var showPaletteSheet by remember { mutableStateOf(false) }
    var showThemeModeSheet by remember { mutableStateOf(false) }
    var showColorSpecSheet by remember { mutableStateOf(false) }

    val backdrop = rememberMaterial3BlurBackdrop(uiState.useBlur)

    if (showPaletteSheet) {
        SelectionSheet(
            title = stringResource(R.string.theme_settings_palette_style),
            items = PaletteStyle.entries,
            selectedItem = uiState.paletteStyle,
            onDismiss = { showPaletteSheet = false },
            onConfirm = { style: PaletteStyle ->
                viewModel.dispatch(ThemeSettingsAction.SetPaletteStyle(style))
            },
            itemLabel = { style: PaletteStyle -> style.displayName },
            backdrop = backdrop,
            useLiquidGlass = uiState.useLiquidGlassBottomSheet,
            liquidGlassBlurRadius = uiState.liquidGlassBlurRadius.dp
        )
    }

    if (showThemeModeSheet) {
        SelectionSheet(
            title = stringResource(R.string.theme_settings_theme_mode),
            items = ThemeMode.entries,
            selectedItem = uiState.themeMode,
            onDismiss = { showThemeModeSheet = false },
            onConfirm = { mode: ThemeMode ->
                viewModel.dispatch(ThemeSettingsAction.SetThemeMode(mode))
            },
            itemLabel = { mode: ThemeMode ->
                when (mode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                }
            },
            backdrop = backdrop,
            useLiquidGlass = uiState.useLiquidGlassBottomSheet,
            liquidGlassBlurRadius = uiState.liquidGlassBlurRadius.dp
        )
    }

    if (showColorSpecSheet) {
        val availableSpecs =
            if (uiState.paletteStyle.supportsSpec2025) ThemeColorSpec.entries else listOf(
                ThemeColorSpec.SPEC_2021
            )
        SelectionSheet(
            title = stringResource(R.string.theme_settings_color_spec),
            items = availableSpecs,
            selectedItem = uiState.colorSpec,
            onDismiss = { showColorSpecSheet = false },
            onConfirm = { spec: ThemeColorSpec ->
                viewModel.dispatch(ThemeSettingsAction.SetColorSpec(spec))
            },
            itemLabel = { spec: ThemeColorSpec -> spec.displayName },
            backdrop = backdrop,
            useLiquidGlass = uiState.useLiquidGlassBottomSheet,
            liquidGlassBlurRadius = uiState.liquidGlassBlurRadius.dp
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = windowInsetsSides?.let { ScaffoldDefaults.contentWindowInsets.only(it) }
            ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            Box {
                TopAppBar(
                    modifier = Modifier.appBarBlurEffect(
                        backdrop = backdrop,
                        useProgressiveBlur = uiState.useProgressiveBlurTopAppBar
                    ),
                    windowInsets = windowInsetsSides?.let { TopAppBarDefaults.windowInsets.only(it) }
                        ?: TopAppBarDefaults.windowInsets,
                    title = {},
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backdrop.getMaterial3AppBarColor(),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                    )
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .windowInsetsPadding(
                            (windowInsetsSides?.let { TopAppBarDefaults.windowInsets.only(it) }
                                ?: TopAppBarDefaults.windowInsets)
                                .only(WindowInsetsSides.Top)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    FloatingTabRow(
                        tabs = settingTabs,
                        selectedIndex = uiState.selectedTab,
                        onSelected = { viewModel.dispatch(ThemeSettingsAction.SetSelectedTab(it)) },
                        icons = settingIcons,
                        containerColor = if (uiState.useTabRowTransparentBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHighest, // surfaceContainerHighest 稍暗一点
                        alignment = if (uiState.useTabRowCenterAlignment) FloatingTabRowAlignment.CENTER else FloatingTabRowAlignment.START,
                        widthMode = if (uiState.useTabRowFillWidth) FloatingTabRowWidthMode.FILL else FloatingTabRowWidthMode.WRAP_CONTENT,
                        indicatorPadding = 4.dp,
                    )
                }
            }
        },
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState.selectedTab,
            label = "settings_tab_content",
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            }
        ) { targetTab ->
            when (targetTab) {
                0 -> {
                    SettingsTab(
                        uiState = uiState,
                        viewModel = viewModel,
                        paddingValues = paddingValues,
                        outerPadding = outerPadding,
                        backdrop = backdrop,
                        onShowThemeModeSheet = { showThemeModeSheet = true },
                        onShowPaletteSheet = { showPaletteSheet = true },
                        onShowColorSpecSheet = { showColorSpecSheet = true }
                    )
                }

                1 -> {
                    AboutTab(
                        paddingValues = paddingValues,
                        outerPadding = outerPadding,
                        backdrop = backdrop,
                    )
                }

                2 -> {
                    AboutTab(
                        paddingValues = paddingValues,
                        outerPadding = outerPadding,
                        backdrop = backdrop,
                    )
                }
            }
        }
    }
}
