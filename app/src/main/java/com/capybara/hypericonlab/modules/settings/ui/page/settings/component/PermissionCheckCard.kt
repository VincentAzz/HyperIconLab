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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.modules.build.domain.packaging.ApkInstallFacade
import org.koin.compose.koinInject


@Composable
fun PermissionCheckCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val apkInstaller = koinInject<ApkInstallFacade>()
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationGranted by remember {
        mutableStateOf(checkNotificationPermission(context))
    }

    var storageGranted by remember {
        mutableStateOf(checkStoragePermission(context))
    }

    var installPackagesGranted by remember {
        mutableStateOf(apkInstaller.canInstallUnknownSources())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        storageGranted = granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = checkNotificationPermission(context)
                storageGranted = checkStoragePermission(context)
                installPackagesGranted = apkInstaller.canInstallUnknownSources()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val needShowCard = !notificationGranted || !storageGranted || !installPackagesGranted

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationGranted) {
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.permission_check_notification_title),
                        description = stringResource(R.string.permission_check_notification_desc),
                        onClick = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        trailingContent = {
                            PrimaryActionButton(
                                text = stringResource(R.string.permission_check_grant),
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            )
                        }
                    )
                }
            }

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
                            PrimaryActionButton(
                                text = stringResource(R.string.permission_check_grant),
                                onClick = {
                                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            )
                        }
                    )
                }
            }

            if (!installPackagesGranted) {
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.permission_check_install_title),
                        description = stringResource(R.string.permission_check_install_desc),
                        onClick = { apkInstaller.openUnknownSourcesSettings() },
                        trailingContent = {
                            PrimaryActionButton(
                                text = stringResource(R.string.permission_check_grant),
                                onClick = { apkInstaller.openUnknownSourcesSettings() }
                            )
                        }
                    )
                }
            }
        }
    }
}

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
