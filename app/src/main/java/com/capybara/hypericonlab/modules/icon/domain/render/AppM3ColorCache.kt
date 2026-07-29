package com.capybara.hypericonlab.modules.icon.domain.render

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.dynamicColorScheme
import com.capybara.hypericonlab.modules.icon.domain.render.AppM3CacheStorage.PersistedColors
import kotlinx.coroutines.yield
import java.util.concurrent.ConcurrentHashMap

// App-M3 颜色方案缓存：内存层 + 文件持久化
// 仅缓存 monet 变体取色所需的 4 个关键颜色，避免存储完整 ColorScheme
// 持久化文件跨启动复用，第二次启动后基本全命中
object AppM3ColorCache {

    // 内存缓存：种子色 Int -> 4 个关键颜色
    private val memoryCache = ConcurrentHashMap<Int, PersistedColors>()

    // 当前持久化配置（paletteStyle/colorSpec 变化时需调用 clear）
    @Volatile
    private var currentPaletteStyle: PaletteStyle? = null

    @Volatile
    private var currentColorSpec: ThemeColorSpec? = null

    // 获取或计算关键颜色：内存命中则返回，否则计算 + 更新内存 + 追加写入文件
    fun getOrCompute(
        context: Context,
        seedColor: Int,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec
    ): PersistedColors {
        ensureConfig(context, paletteStyle, colorSpec)

        return memoryCache.getOrPut(seedColor) {
            val colors = computeColors(seedColor, paletteStyle, colorSpec)
            // 追加写入持久化文件
            AppM3CacheStorage.append(context, paletteStyle, colorSpec, colors)
            colors
        }
    }

    // 仅从内存获取（不触发计算），用于快速查询是否已缓存
    fun getIfCached(seedColor: Int): PersistedColors? {
        return memoryCache[seedColor]
    }

    // 启动时异步加载持久化文件到内存
    fun loadFromFile(
        context: Context,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec
    ) {
        ensureConfig(context, paletteStyle, colorSpec)
        val loaded = AppM3CacheStorage.load(context, paletteStyle, colorSpec)
        if (loaded.isNotEmpty()) {
            memoryCache.putAll(loaded)
        }
    }

    // 预处理全部种子色：跳过已缓存项，分批计算并持久化
    // 每批 BATCH_SIZE 个，批次间 yield 让出 CPU，避免阻塞主线程
    suspend fun preprocessAll(
        context: Context,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec,
        seedColors: Collection<Int>,
        onProgress: (computed: Int, total: Int) -> Unit
    ) {
        ensureConfig(context, paletteStyle, colorSpec)
        // 已持久化的种子色（包含内存和文件中的）
        val persistedSeeds = AppM3CacheStorage.loadSeedColors(context, paletteStyle, colorSpec)
        val toCompute = seedColors.filter { it !in persistedSeeds && it !in memoryCache.keys }
        val total = seedColors.size
        var computed = total - toCompute.size // 已完成数量

        // 上报初始进度（已持久化的部分）
        onProgress(computed, total)

        // 分批计算
        val batch = ArrayList<PersistedColors>(BATCH_SIZE)
        for ((index, seedColor) in toCompute.withIndex()) {
            val colors = computeColors(seedColor, paletteStyle, colorSpec)
            memoryCache[seedColor] = colors
            batch.add(colors)

            // 攒满一批批量写入
            if (batch.size >= BATCH_SIZE) {
                AppM3CacheStorage.appendBatch(context, paletteStyle, colorSpec, batch.toList())
                batch.clear()
            }

            computed++
            // 每 PROGRESS_REPORT_INTERVAL 个上报一次进度
            if (computed % PROGRESS_REPORT_INTERVAL == 0 || computed == total) {
                onProgress(computed, total)
            }
            // 每批让出 CPU
            if (index % BATCH_SIZE == 0) yield()
        }
        // 写入剩余不足一批的记录
        if (batch.isNotEmpty()) {
            AppM3CacheStorage.appendBatch(context, paletteStyle, colorSpec, batch.toList())
            batch.clear()
        }
        // 最终进度上报
        onProgress(computed, total)
    }

    // 预处理 appColorSchemes：提取所有唯一背景色作为种子色
    // reduceWhiteBg 启用时，白色背景改用前景色作为种子色（与运行时 resolveAppM3Colors 逻辑一致）
    suspend fun preprocessAppColorSchemes(
        context: Context,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec,
        appColorSchemes: Map<String, Pair<String, String>>,
        reduceWhiteBg: Boolean,
        onProgress: (computed: Int, total: Int) -> Unit
    ) {
        // 提取唯一种子色：reduceWhiteBg 时白色背景改用前景色
        val seedColors = mutableSetOf<Int>()
        appColorSchemes.values.forEach { (fgHex, bgHex) ->
            val seedHex =
                if (reduceWhiteBg && ConfigColorResolver.isColorWhite(bgHex)) fgHex else bgHex
            try {
                seedColors.add(seedHex.toColorInt())
            } catch (_: Exception) {
                // 非法颜色值跳过
            }
        }
        preprocessAll(context, paletteStyle, colorSpec, seedColors, onProgress)
    }

    // 清空内存 + 删除持久化文件（paletteStyle/colorSpec 变化时调用）
    fun clear(context: Context) {
        memoryCache.clear()
        AppM3CacheStorage.clear(context)
        currentPaletteStyle = null
        currentColorSpec = null
    }

    // 当前缓存的种子色数量（用于进度展示）
    fun cachedSeedCount(): Int = memoryCache.size

    // 确保配置一致，若 paletteStyle/colorSpec 变化则清空缓存
    private fun ensureConfig(
        context: Context,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec
    ) {
        if (currentPaletteStyle != paletteStyle || currentColorSpec != colorSpec) {
            if (currentPaletteStyle != null) {
                // 配置变化，清空旧缓存
                memoryCache.clear()
                AppM3CacheStorage.clear(context)
            }
            currentPaletteStyle = paletteStyle
            currentColorSpec = colorSpec
        }
    }

    // 计算 light/dark scheme 并提取 4 个关键颜色
    private fun computeColors(
        seedColor: Int,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec
    ): PersistedColors {
        val color = Color(seedColor)
        val lightScheme = try {
            dynamicColorScheme(
                keyColor = color,
                isDark = false,
                style = paletteStyle,
                colorSpec = colorSpec
            )
        } catch (e: Exception) {
            dynamicColorScheme(keyColor = Color.Blue, isDark = false)
        }

        val darkScheme = try {
            dynamicColorScheme(
                keyColor = color,
                isDark = true,
                style = paletteStyle,
                colorSpec = colorSpec
            )
        } catch (e: Exception) {
            dynamicColorScheme(keyColor = Color.Blue, isDark = true)
        }

        return extractKeyColors(seedColor, lightScheme, darkScheme)
    }

    // 从 light/dark ColorScheme 提取 monet 变体取色所需的 4 个关键颜色
    private fun extractKeyColors(
        seedColor: Int,
        lightScheme: ColorScheme,
        darkScheme: ColorScheme
    ): PersistedColors {
        return PersistedColors(
            seedColor = seedColor,
            // 浅色 fg / 中性 bg
            lightPrimary = lightScheme.primary.toArgb(),
            // 浅色 bg / 中性 fg
            lightPrimaryContainer = lightScheme.primaryContainer.toArgb(),
            // 暗色 fg
            darkOnPrimaryContainer = darkScheme.onPrimaryContainer.toArgb(),
            // 暗色 bg
            darkOnPrimary = darkScheme.onPrimary.toArgb()
        )
    }

    // 预处理调参常量
    private object PreprocessConfig {
        // 分批计算大小
        const val BATCH_SIZE = 20

        // 进度上报间隔
        const val PROGRESS_REPORT_INTERVAL = 10
    }

    val BATCH_SIZE = PreprocessConfig.BATCH_SIZE
    val PROGRESS_REPORT_INTERVAL = PreprocessConfig.PROGRESS_REPORT_INTERVAL
}
