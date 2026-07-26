package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
        // tab 切换：0=预览，1=参数；默认预览
        var selectedTab by remember { mutableIntStateOf(0) }

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
                        selectedIndex = selectedTab,
                        onSelected = { selectedTab = it },
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

            // 内容卡片：圆角与边缘间距参考 LogSheet/LawniconsBrowserSheet 内部卡片
            // 使用 AnimatedContent 实现 tab 切换时的淡入淡出过渡
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp, 0.dp, 8.dp, 8.dp),
                label = "previewTabTransition"
            ) { tab ->
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = rememberKyantRoundedRectangleShape(ExtraLargeRadius - 8.dp),
                    color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f)
                ) {
                    if (tab == 0) {
                        // 预览 tab：图片裁切填满卡片
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "全屏预览",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 参数 tab：等宽字体展示 IconBuildConfig 多行带缩进文本
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

