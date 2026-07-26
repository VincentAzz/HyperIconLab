package com.capybara.hypericonlab.modules.icon.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.modules.icon.domain.model.WallpaperUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 壁纸管理器
class WallpaperManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val configProvider: () -> WallpaperUiState,
    private val onWallpaperUpdated: () -> Unit
) {
    private val _wallpaperBitmap = MutableStateFlow<Bitmap?>(null)
    val wallpaperBitmap: StateFlow<Bitmap?> = _wallpaperBitmap.asStateFlow()

    // 当前壁纸配色方案
    private val _wallpaperColorScheme =
        MutableStateFlow<MonetColorExtractor.WallpaperColorScheme?>(null)
    val wallpaperColorScheme: StateFlow<MonetColorExtractor.WallpaperColorScheme?> =
        _wallpaperColorScheme.asStateFlow()

    // 加载 assets 默认壁纸
    fun loadDefaultWallpaper() {
        scope.launch(Dispatchers.IO) {
            try {
                context.assets.open("wallpapers/wallpaper_fallback.jpg").use {
                    BitmapFactory.decodeStream(it)?.let { bmp -> updateWallpaper(bmp) }
                }
            } catch (_: Exception) {
            }
        }
    }

    // 选择图片，从 Uri 加载壁纸
    fun updateWallpaperFromUri(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)?.let { bmp -> updateWallpaper(bmp) }
                }
            } catch (_: Exception) {
            }
        }
    }

    // 更新壁纸位图并重提取配色，触发预览重生成
    fun updateWallpaper(bmp: Bitmap) {
        _wallpaperBitmap.value = bmp
        reextractWallpaperColors()
        onWallpaperUpdated()
    }

    // 重新提取壁纸配色（壁纸或配置变化时调用）
    fun reextractWallpaperColors() {
        val bmp = _wallpaperBitmap.value ?: return
        val wp = configProvider()
        _wallpaperColorScheme.value = MonetColorExtractor.extractFromBitmap(
            bitmap = bmp,
            paletteStyle = wp.paletteStyle,
            colorSpec = wp.colorSpec
        )
    }
}
