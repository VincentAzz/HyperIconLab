package com.capybara.hypericonlab.modules.settings.ui.page.settings.tabs

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.modules.settings.ui.page.settings.component.AboutCard
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop


@Composable
fun AboutTab(
    paddingValues: PaddingValues,
    outerPadding: PaddingValues,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection) +
                    outerPadding.calculateStartPadding(layoutDirection),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(layoutDirection) +
                    outerPadding.calculateEndPadding(layoutDirection),
            bottom = outerPadding.calculateBottomPadding(),
        ),
    ) {
        item(key = "aboutCard") {
            AboutCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }


        item(key = "Developer") {
            Spacer(modifier = Modifier.height(16.dp))
            SegmentedColumn(title = "开发者") {
                item {
                    BaseItemContainer {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }
            }
        }

        item(key = "Repository") {
            SegmentedColumn(title = "仓库") {
                item {
                    BaseItemContainer {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }
            }
        }

        item(key = "OpenSource") {
            SegmentedColumn(title = "开源引用") {
                item {
                    BaseItemContainer {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }
                item {
                    BaseItemContainer {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }
                item {
                    BaseItemContainer {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }
                item {
                    BaseItemContainer {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }
                item {
                    BaseItemContainer {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }
                item {
                    BaseItemContainer {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            item(key = "Tip") {
                SegmentedColumn(title = "提示") {
                    item {
                        BaseItemContainer {
                            Text(
                                text = "当前系统不支持流光效果，需 Android 13+",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }

        item(key = "navPadding") {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
