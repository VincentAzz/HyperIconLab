package com.capybara.hypericonlab.modules.icon.ui.page.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRow
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowAlignment
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowWidthMode
import com.capybara.hypericonlab.core.designsystem.liquidglass.appBarBlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.getMaterial3AppBarColor
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 任务页面：展示活动任务与已完成任务列表，并承载 [TaskDetailSheet]。
 *
 * 布局：
 * - 顶部 TopAppBar 内嵌 FloatingTabRow（"进行中"/"已完成"两 tab），与自定义页面风格一致
 * - LazyColumn：根据当前 tab 展示活动任务或已完成任务
 * - 空列表展示对应空态文案
 * - 卡片点击：设置内部 selectedTaskId 状态，渲染 [TaskDetailSheet]
 *
 * @param onTaskClick 可选外部回调（用于上层联动）；默认空实现，页面内部已自带 sheet 渲染
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TaskPage(
    modifier: Modifier = Modifier,
    viewModel: IconViewModel = koinViewModel(),
    themeViewModel: SettingsViewModel = koinViewModel(),
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null,
    onTaskClick: (String) -> Unit = {}
) {
    val activeTasks by viewModel.activeBuildTasks.collectAsStateWithLifecycle()
    val finishedTasks by viewModel.finishedBuildTasks.collectAsStateWithLifecycle()
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()

    // 当前选中的 tab（0 = 进行中，1 = 已完成），使用 rememberSaveable 跨重组保持
    var selectedTab by rememberSaveable { mutableStateOf(TaskPageConfig.DEFAULT_TAB_INDEX) }

    // 当前选中的任务 id（点击卡片时设置，详情 sheet 关闭时清空）
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val backdrop = rememberMaterial3BlurBackdrop(themeState.useBlur)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = windowInsetsSides?.let { ScaffoldDefaults.contentWindowInsets.only(it) }
            ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            // Box 叠加：TopAppBar 在底，FloatingTabRow 浮于其上（与 CustomPage 一致）
            Box {
                TopAppBar(
                    modifier = Modifier.appBarBlurEffect(
                        backdrop = backdrop,
                        useProgressiveBlur = themeState.useProgressiveBlurTopAppBar
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
                        .padding(horizontal = TaskPageConfig.TABROW_HORIZONTAL_PADDING),
                    contentAlignment = Alignment.CenterStart
                ) {
                    FloatingTabRow(
                        tabs = listOf("进行中", "已完成"),
                        selectedIndex = selectedTab,
                        onSelected = { selectedTab = it },
                        indicatorPadding = TaskPageConfig.TABROW_INDICATOR_PADDING,
                        containerColor = if (themeState.useTabRowTransparentBackground) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        alignment = if (themeState.useTabRowCenterAlignment) FloatingTabRowAlignment.CENTER
                        else FloatingTabRowAlignment.START,
                        widthMode = if (themeState.useTabRowFillWidth) FloatingTabRowWidthMode.FILL
                        else FloatingTabRowWidthMode.WRAP_CONTENT
                    )
                }
            }
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        // 根据当前 tab 决定展示的列表
        val displayTasks =
            if (selectedTab == TaskPageConfig.TAB_INDEX_ACTIVE) activeTasks else finishedTasks
        // 关键：参照设置页实现，layerBackdrop 必须放在最外层（不带 padding），
        // 让 backdrop 捕获区域覆盖整个 Scaffold content（含 TopAppBar 下方位置），
        // TopAppBar 通过 appBarBlurEffect 采样 backdrop 时才能取到内容；
        // 所有 padding 通过 contentPadding 实现，不挤占 backdrop 捕获区域。
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection) +
                        outerPadding.calculateStartPadding(layoutDirection) +
                        TaskPageConfig.HORIZONTAL_PADDING,
                top = paddingValues.calculateTopPadding() + TaskPageConfig.VERTICAL_PADDING,
                end = paddingValues.calculateEndPadding(layoutDirection) +
                        outerPadding.calculateEndPadding(layoutDirection) +
                        TaskPageConfig.HORIZONTAL_PADDING,
                bottom = TaskPageConfig.VERTICAL_PADDING + outerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(TaskPageConfig.CARD_SPACING)
        ) {
            if (displayTasks.isNotEmpty()) {
                items(displayTasks, key = { it.taskId }) { task ->
                    TaskCard(
                        task = task,
                        onClick = {
                            selectedTaskId = task.taskId
                            onTaskClick(task.taskId)
                        }
                    )
                }
            } else {
                // 空态文案：根据 tab 显示不同提示
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = TaskPageConfig.EMPTY_STATE_TOP_PADDING),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (selectedTab == TaskPageConfig.TAB_INDEX_ACTIVE)
                                    "暂无进行中的任务"
                                else "暂无已完成的任务",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(TaskPageConfig.EMPTY_STATE_TEXT_GAP))
                            Text(
                                text = if (selectedTab == TaskPageConfig.TAB_INDEX_ACTIVE)
                                    "前往「自定义」页面构建图标包"
                                else "完成构建的任务将显示在此处",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // 详情 sheet：选中任务 id 时渲染，从活动 + 已完成列表中查找最新任务数据
    val selectedTask = selectedTaskId?.let { id ->
        activeTasks.find { it.taskId == id } ?: finishedTasks.find { it.taskId == id }
    }
    selectedTask?.let { task ->
        TaskDetailSheet(
            task = task,
            onStop = { viewModel.cancelBuildTask(task.taskId) },
            onDelete = { viewModel.deleteFinishedBuildTask(task.taskId) },
            onRetry = { viewModel.retryBuildTask(task.taskId) },
            onDismiss = { selectedTaskId = null },
            backdrop = backdrop,
            // 跟随应用的 BottomSheet 液态玻璃开关，而非全局模糊开关 useBlur
            useLiquidGlass = themeState.useLiquidGlassBottomSheet,
            liquidGlassBlurRadius = themeState.liquidGlassBlurRadius.dp
        )
    }
}

// 任务页面关键参数集中声明，便于调参
private object TaskPageConfig {
    // 默认 tab 索引（0 = 进行中）
    const val DEFAULT_TAB_INDEX = 0

    // 进行中 tab 索引
    const val TAB_INDEX_ACTIVE = 0

    // 列表水平内边距
    val HORIZONTAL_PADDING = 16.dp

    // 列表垂直内边距
    val VERTICAL_PADDING = 8.dp

    // 卡片间距
    val CARD_SPACING = 12.dp

    // FloatingTabRow 水平边距
    val TABROW_HORIZONTAL_PADDING = 16.dp

    // FloatingTabRow indicator padding
    val TABROW_INDICATOR_PADDING = 4.dp

    // 空态顶部留白
    val EMPTY_STATE_TOP_PADDING = 120.dp

    // 空态主副文案间距
    val EMPTY_STATE_TEXT_GAP = 8.dp
}
