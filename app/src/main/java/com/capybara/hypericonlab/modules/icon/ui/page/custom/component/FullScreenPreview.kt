package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRow
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowAlignment
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowWidthMode
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPreview(
    show: Boolean,
    bitmap: Bitmap?,
    configText: String,
    onDismiss: () -> Unit,
    horizontalPadding: Dp = 8.dp,
    bottomPadding: Dp = 4.dp,
    cornerRadius: Dp = ExtraLargeRadius,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp,
) {
    if (show && bitmap != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()
        val pagerState = rememberPagerState(pageCount = { 2 })

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
                title = {
                    FloatingTabRow(
                        tabs = listOf("预览", "参数"),
                        selectedIndex = pagerState.currentPage,
                        onSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        barHeight = 40.dp,
                        alignment = FloatingTabRowAlignment.CENTER,
                        widthMode = FloatingTabRowWidthMode.WRAP_CONTENT,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                },
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
                            .padding(end = 12.dp)
                            .size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                AppMaterialSymbols.check,
                                contentDescription = "确定",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            )


            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp, 0.dp, 8.dp, 8.dp),
                shape = rememberKyantRoundedRectangleShape(ExtraLargeRadius - 8.dp),
                color = Color.Transparent
            ) {
                // 禁用 overscroll effect：避免滑动力度大时到达边界页触发回弹抖动
                CompositionLocalProvider(LocalOverscrollFactory provides null) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(rememberKyantRoundedRectangleShape(ExtraLargeRadius - 8.dp)),
                        pageSpacing = 8.dp
                    ) { page ->
                        val pageOffset = (
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                ).absoluteValue
                        val fadeAlpha = (1f - pageOffset * 0.4f).coerceIn(0.75f, 1f)

                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { this.alpha = fadeAlpha },
                            shape = rememberKyantRoundedRectangleShape(ExtraLargeRadius - 8.dp),
                            color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.9f)
                        ) {
                            when (page) {
                                0 -> {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "全屏预览",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                1 -> {
                                    val verticalScrollState = rememberScrollState()
                                    SelectionContainer {
                                        Text(
                                            text = configText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = GoogleSansCodeFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            softWrap = true,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(verticalScrollState)
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


