package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.arrow_downward
import com.capybara.hypericonlab.core.designsystem.symbol.clear_all
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.logging.LogEntry
import com.capybara.hypericonlab.core.logging.LogType
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(
    viewModel: IconViewModel,
    onDismiss: () -> Unit,
    horizontalPadding: Dp = 8.dp,
    bottomPadding: Dp = 4.dp,
    cornerRadius: Dp = ExtraLargeRadius,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp,
) {
    val logs by viewModel.logs.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isAtTop by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex == 0) && (listState.firstVisibleItemScrollOffset == 0)
        }
    }
    val showScrollHint by remember {
        derivedStateOf {
            isAtTop && listState.canScrollForward
        }
    }

    FloatingBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        horizontalPadding = horizontalPadding,
        bottomPadding = bottomPadding,
        cornerRadius = cornerRadius,
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass,
        liquidGlassBlurRadius = liquidGlassBlurRadius,
    ) {
        CenterAlignedTopAppBar(
            title = { SheetTitle("运行日志") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            navigationIcon = {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            },
            actions = {
                Surface(
                    onClick = { viewModel.clearLogs() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.clear_all,
                            contentDescription = "清除日志",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp, 0.dp, 8.dp, 8.dp),
                shape = rememberKyantRoundedRectangleShape(ExtraLargeRadius - 8.dp),
                color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs.reversed()) { entry ->
                            LogItem(entry)
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollHint,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppMaterialSymbols.arrow_downward,
                                    contentDescription = "向下滚动",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(entry: LogEntry) {
    val color = when (entry.type) {
        LogType.INFO -> MaterialTheme.colorScheme.onSurface
        LogType.ERROR -> MaterialTheme.colorScheme.error
        LogType.SUCCESS -> Color(0xFF4CAF50)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[${entry.formattedTime}]",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = GoogleSansCodeFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = entry.type.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                fontFamily = GoogleSansCodeFontFamily,
                color = color
            )
            entry.duration?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "[$it]",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = GoogleSansCodeFontFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = GoogleSansCodeFontFamily,
            color = color,
            modifier = Modifier.padding(top = 2.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
