package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRow
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowAlignment
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowWidthMode
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.MiuixThemeBridge
import com.capybara.hypericonlab.core.designsystem.theme.currentSheetRoundedLayout
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.ColorSpace
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    initialColor: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit,
    horizontalPadding: Dp = 8.dp,
    bottomPadding: Dp = 4.dp,
    cornerRadius: Dp? = null,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
) {
    val roundedLayout = currentSheetRoundedLayout()
    val initialArgb = remember(initialColor) {
        try {
            Color(initialColor.toColorInt()).toArgb()
        } catch (_: Exception) {
            Color.Black.toArgb()
        }
    }
    var currentColor by remember { mutableStateOf(Color(initialArgb)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var hexInput by remember { mutableStateOf(initialColor.removePrefix("#").uppercase()) }
    var isUserTyping by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(currentColor) {
        if (!isUserTyping) {
            hexInput = String.format("%08X", currentColor.toArgb()).uppercase()
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
        fillMaxHeight = false,
    ) {
        CenterAlignedTopAppBar(
            title = {
                FloatingTabRow(
                    tabs = listOf("滑块", "色板"),
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
                    onClick = onDismiss,
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
                        val finalHex = String.format("%08X", currentColor.toArgb())
                        onColorSelected("#$finalHex")
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = roundedLayout.cardInset)
                .padding(bottom = roundedLayout.cardInset)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = rememberKyantRoundedRectangleShape(roundedLayout.cardCornerRadius),
                color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f)
            ) {
                MiuixThemeBridge {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (selectedTab == 0) {
                            ColorPicker(
                                color = currentColor,
                                onColorChanged = { currentColor = it },
                                showPreview = true,
                                colorSpace = ColorSpace.HSV
                            )
                        } else {
                            ColorPalette(
                                color = currentColor,
                                onColorChanged = { currentColor = it },
                                showPreview = true
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = hexInput,
                            onValueChange = { input ->
                                if (input.length <= 8 && input.all {
                                        it.isDigit() || it.uppercaseChar() in 'A'..'F'
                                    }) {
                                    isUserTyping = true
                                    val upperHex = input.uppercase()
                                    hexInput = upperHex
                                    if (upperHex.length == 6 || upperHex.length == 8) {
                                        try {
                                            val fullHex =
                                                if (upperHex.length == 6) "FF$upperHex" else upperHex
                                            currentColor = Color("#$fullHex".toColorInt())
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii
                            ),
                            leadingIcon = {
                                Text(
                                    "HEX = #",
                                    style = MiuixTheme.textStyles.main.copy(
                                        color = MiuixTheme.colorScheme.onSurface,
                                        fontFamily = GoogleSansCodeFontFamily,
                                    ),
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            },
                            textStyle = MiuixTheme.textStyles.main.copy(
                                color = MiuixTheme.colorScheme.onSurface,
                                fontFamily = GoogleSansCodeFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
