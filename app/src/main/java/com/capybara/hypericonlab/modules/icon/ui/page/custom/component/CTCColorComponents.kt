package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.BaseWidgetAction
import com.capybara.hypericonlab.core.designsystem.component.BaseWidgetActionIcon
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.StyleChip
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.color_lens
import com.capybara.hypericonlab.core.designsystem.symbol.swap_horiz
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.LargeCardRadius
import com.capybara.hypericonlab.core.designsystem.theme.ctc.CTCPresets
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel

@Composable
fun CTCColorSwatchPreviewIcon(
    bgName: String,
    bgHex: String,
    primaryName: String,
    primaryHex: String,
    isSelected: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    onClick: () -> Unit
) {
    val bgColor = Color(bgHex.toColorInt())
    val primaryColor = Color(primaryHex.toColorInt())
    val squircleBackgroundColor = primaryColor.copy(alpha = 0.2f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(rememberKyantRoundedRectangleShape(LargeCardRadius))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = squircleBackgroundColor,
                    shape = rememberKyantRoundedRectangleShape(CornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = bgColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppMaterialSymbols.check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = bgName,
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = primaryName,
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CTCConfigSection(viewModel: IconViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val ctcType = config.ctc.type
    val ctcVariant = config.ctc.variant
    val ctcSelectedIndex = config.ctc.selectedIndex
    val context = LocalContext.current

    SegmentedColumn(
        contentPadding = PaddingValues(0.dp)
    ) {
        item { shape ->
            BaseItemContainer(shape = shape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(12.dp),
                ) {
                    StyleChip(
                        label = "同色系",
                        selected = ctcType == "monochromatic",
                        onClick = {
                            viewModel.updateConfig { c ->
                                c.copy(
                                    ctc = c.ctc.copy(
                                        type = "monochromatic",
                                        variant = "light",
                                        selectedIndex = 0
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StyleChip(
                        label = "撞色",
                        selected = ctcType == "contrast",
                        onClick = {
                            viewModel.updateConfig { c ->
                                c.copy(
                                    ctc = c.ctc.copy(
                                        type = "contrast",
                                        variant = "normal",
                                        selectedIndex = 0
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            }
        }

        item { shape ->
            BaseItemContainer(shape = shape) {
                if (ctcType == "monochromatic") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(12.dp)
                    ) {
                        StyleChip(
                            label = "浅色",
                            selected = ctcVariant == "light",
                            onClick = {
                                viewModel.updateConfig { c ->
                                    c.copy(
                                        ctc = c.ctc.copy(
                                            variant = "light"
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        StyleChip(
                            label = "暗色",
                            selected = ctcVariant == "dark",
                            onClick = {
                                viewModel.updateConfig { c ->
                                    c.copy(
                                        ctc = c.ctc.copy(
                                            variant = "dark"
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        StyleChip(
                            label = "中性",
                            selected = ctcVariant == "neutral",
                            onClick = {
                                viewModel.updateConfig { c ->
                                    c.copy(
                                        ctc = c.ctc.copy(
                                            variant = "neutral"
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("交换背景和主题色", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = {
                            viewModel.updateConfig { c -> c.copy(ctc = c.ctc.copy(variant = if (ctcVariant == "normal") "swapped" else "normal")) }
                        }) {
                            Icon(AppMaterialSymbols.swap_horiz, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("交换")
                        }
                    }
                }
            }
        }

        item { shape ->
            BaseItemContainer(shape = shape) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    val columns = 3
                    if (ctcType == "monochromatic") {
                        val schemes = CTCPresets.MonochromaticSchemes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            schemes.chunked(columns).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { scheme ->
                                        val bg = when (ctcVariant) {
                                            "light" -> scheme.lightBg
                                            "dark" -> scheme.darkBg
                                            "neutral" -> scheme.neutralBg
                                            else -> scheme.lightBg
                                        }
                                        val primary = when (ctcVariant) {
                                            "light" -> scheme.lightPrimary
                                            "dark" -> scheme.darkPrimary
                                            "neutral" -> scheme.neutralPrimary
                                            else -> scheme.lightPrimary
                                        }
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CTCColorSwatchPreviewIcon(
                                                bgName = bg.name,
                                                bgHex = bg.hex,
                                                primaryName = primary.name,
                                                primaryHex = primary.hex,
                                                isSelected = schemes.indexOf(scheme) == ctcSelectedIndex,
                                                textStyle = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp
                                                ),
                                                textColor = MaterialTheme.colorScheme.onSurface,
                                                onClick = {
                                                    viewModel.updateConfig { c ->
                                                        c.copy(
                                                            ctc = c.ctc.copy(
                                                                selectedIndex = schemes.indexOf(
                                                                    scheme
                                                                )
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    } else {
                        val schemes = CTCPresets.ContrastSchemes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            schemes.chunked(columns).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { scheme ->
                                        val isSwapped = ctcVariant == "swapped"
                                        val bg = if (isSwapped) scheme.primary else scheme.bg
                                        val primary = if (isSwapped) scheme.bg else scheme.primary
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CTCColorSwatchPreviewIcon(
                                                bgName = bg.name,
                                                bgHex = bg.hex,
                                                primaryName = primary.name,
                                                primaryHex = primary.hex,
                                                isSelected = schemes.indexOf(scheme) == ctcSelectedIndex,
                                                textStyle = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp
                                                ),
                                                textColor = MaterialTheme.colorScheme.onSurface,
                                                onClick = {
                                                    viewModel.updateConfig { c ->
                                                        c.copy(
                                                            ctc = c.ctc.copy(
                                                                selectedIndex = schemes.indexOf(
                                                                    scheme
                                                                )
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            BaseWidget(
                icon = AppMaterialSymbols.color_lens,
                // iconPlaceholder = false,
                title = "中华传统色资料馆",
                description = "探索更多配色方案",
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://colors.xiaoxiaodong.ai/index.html".toUri()
                    )
                    context.startActivity(intent)
                },
                trailingContent = {
                    BaseWidgetAction(icon = BaseWidgetActionIcon.ARROW_OUTWARD)
                }
            )
        }
    }
}
