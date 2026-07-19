package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn

/**
 * 权限检查卡片：仅在权限未授权时显示，已授权则隐藏。
 *
 * 采纳方案 C：设置页可视化卡片 + 首次提交任务时兜底申请（兜底逻辑由 IconViewModel 负责）。
 *
 * 检查项：
 * - 通知权限（Android 13+）：用于显示构建进度通知
 * - 存储权限（Android 9 及以下）：用于将构建产物导出到 Documents 目录
 *
 * 风格：与项目同步开关一致，使用 [BaseWidget] 的 iconPlaceholder=false（不显示图标），
 * 避免新增 symbol 资源。
 *
 * 全部授权时整张卡片不渲染，避免占用设置页空间。
 */
@Composable
fun PermissionCheckCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current

    // 通知权限状态（Android 13+）
    var notificationGranted by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    // 存储权限状态（Android 9 及以下）
    var storageGranted by remember {
        mutableStateOf(checkStoragePermission(context))
    }

    // 申请通知权限的 launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }
    // 申请存储权限的 launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        storageGranted = granted
    }

    // 应用从后台回到前台时，重新检查权限（用户可能去系统设置改了权限）
    LaunchedEffect(Unit) {
        notificationGranted = checkNotificationPermission(context)
        storageGranted = checkStoragePermission(context)
    }

    // 是否需要显示卡片：有任一权限未授权时显示
    val needShowCard = !notificationGranted || !storageGranted

    AnimatedVisibility(
        visible = needShowCard,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(contentPadding)
    ) {
        SegmentedColumn(title = stringResource(R.string.permission_check_title)) {
            // 通知权限条目（Android 13+ 才显示）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationGranted) {
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.permission_check_notification_title),
                        description = stringResource(R.string.permission_check_notification_desc),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        trailingContent = {
                            PermissionGrantButton(
                                text = stringResource(R.string.permission_check_grant),
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                    )
                }
            }

            // 存储权限条目（Android 9 及以下才显示）
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !storageGranted) {
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.permission_check_storage_title),
                        description = stringResource(R.string.permission_check_storage_desc),
                        onClick = {
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        },
                        trailingContent = {
                            PermissionGrantButton(
                                text = stringResource(R.string.permission_check_grant),
                                onClick = {
                                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

// "去授权"按钮（紧凑样式，配合 BaseWidget 的 trailingContent slot）
@Composable
private fun PermissionGrantButton(
    text: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

// 检查通知权限：Android 13+ 检查 POST_NOTIFICATIONS，13 以下默认已授予
private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

// 检查存储权限：Android 9 及以下检查 WRITE_EXTERNAL_STORAGE，10+ 默认无需该权限
private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

