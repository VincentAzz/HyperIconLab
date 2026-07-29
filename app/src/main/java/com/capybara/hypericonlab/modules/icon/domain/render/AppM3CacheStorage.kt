package com.capybara.hypericonlab.modules.icon.domain.render

import android.content.Context
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// App-M3 颜色方案持久化存储
// 仅存储 monet 变体取色所需的 4 个关键颜色，大幅减小文件体积
// 文件格式：文件头(版本号+paletteStyle+colorSpec) + 数据条目(seedColor+4个颜色)
// 每个条目 4 + 4*4 = 20 字节，1200 个唯一色约 24KB
object AppM3CacheStorage {

    // 文件名
    private const val FILE_NAME = "app_m3_cache.bin"

    // 文件版本号（用于格式变更时整体失效）
    private const val FILE_VERSION = 1

    // 每条记录的关键颜色数量（light primary/primaryContainer + dark onPrimaryContainer/onPrimary）
    private const val COLOR_COUNT_PER_RECORD = 4

    // 单条记录字节数：seedColor(4) + 4 个颜色(4*4) = 20
    private const val RECORD_BYTES = 4 + COLOR_COUNT_PER_RECORD * 4

    // 文件写入锁，避免多线程同时 append 导致数据错乱
    private val writeLock = ReentrantLock()

    // 持久化的关键颜色数据
    data class PersistedColors(
        val seedColor: Int,
        // 浅色 fg / 中性 bg
        val lightPrimary: Int,
        // 浅色 bg / 中性 fg
        val lightPrimaryContainer: Int,
        // 暗色 fg
        val darkOnPrimaryContainer: Int,
        // 暗色 bg
        val darkOnPrimary: Int
    )

    // 构造文件头标识（paletteStyle + colorSpec 变化时整体失效）
    private fun buildHeaderKey(paletteStyle: PaletteStyle, colorSpec: ThemeColorSpec): String {
        return "${paletteStyle.name}|${colorSpec.name}"
    }

    // 加载持久化文件到内存
    // 若文件不存在、版本不匹配或解析失败，返回空 Map 并删除损坏文件
    // 文件头中的 recordCount 不准确（append 不更新），按文件长度读取直到 EOF
    fun load(
        context: Context,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec
    ): Map<Int, PersistedColors> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyMap()

        return try {
            DataInputStream(FileInputStream(file)).use { dis ->
                val version = dis.readInt()
                if (version != FILE_VERSION) return emptyMap()

                val headerKey = dis.readUTF()
                if (headerKey != buildHeaderKey(paletteStyle, colorSpec)) return emptyMap()

                // 文件头的 recordCount 字段（不准确，按 EOF 读取）
                dis.readInt()

                val result = HashMap<Int, PersistedColors>()
                val headerKeyBytes = headerKey.toByteArray(Charsets.UTF_8).size
                val totalHeaderSize = 4 + 2 + headerKeyBytes + 4
                val remainingBytes = file.length() - totalHeaderSize
                val recordCount = (remainingBytes / RECORD_BYTES).toInt()

                repeat(recordCount) {
                    val seedColor = dis.readInt()
                    val lightPrimary = dis.readInt()
                    val lightPrimaryContainer = dis.readInt()
                    val darkOnPrimaryContainer = dis.readInt()
                    val darkOnPrimary = dis.readInt()
                    result[seedColor] = PersistedColors(
                        seedColor = seedColor,
                        lightPrimary = lightPrimary,
                        lightPrimaryContainer = lightPrimaryContainer,
                        darkOnPrimaryContainer = darkOnPrimaryContainer,
                        darkOnPrimary = darkOnPrimary
                    )
                }

                result
            }
        } catch (_: Exception) {
            // 文件损坏，删除后回退纯内存模式
            file.delete()
            emptyMap()
        }
    }

    // 追加写入单条记录（文件不存在时自动创建并写入文件头）
    fun append(
        context: Context,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec,
        colors: PersistedColors
    ) = writeLock.withLock {
        val file = File(context.filesDir, FILE_NAME)
        val isNewFile = !file.exists()

        try {
            DataOutputStream(FileOutputStream(file, true)).use { dos ->
                if (isNewFile) {
                    dos.writeInt(FILE_VERSION)
                    dos.writeUTF(buildHeaderKey(paletteStyle, colorSpec))
                    dos.writeInt(0) // 初始记录数为 0，后续 append 不更新
                }
                // 写入单条记录
                dos.writeInt(colors.seedColor)
                dos.writeInt(colors.lightPrimary)
                dos.writeInt(colors.lightPrimaryContainer)
                dos.writeInt(colors.darkOnPrimaryContainer)
                dos.writeInt(colors.darkOnPrimary)
            }
        } catch (_: Exception) {
            // 写入失败不阻塞主流程，下次启动时文件可能损坏会被自动删除
        }
    }

    // 批量写入（用于预处理场景，避免频繁 IO）
    fun appendBatch(
        context: Context,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec,
        records: List<PersistedColors>
    ) = writeLock.withLock {
        if (records.isEmpty()) return@withLock

        val file = File(context.filesDir, FILE_NAME)
        val isNewFile = !file.exists()

        try {
            DataOutputStream(FileOutputStream(file, true)).use { dos ->
                if (isNewFile) {
                    dos.writeInt(FILE_VERSION)
                    dos.writeUTF(buildHeaderKey(paletteStyle, colorSpec))
                    dos.writeInt(0)
                }
                records.forEach { record ->
                    dos.writeInt(record.seedColor)
                    dos.writeInt(record.lightPrimary)
                    dos.writeInt(record.lightPrimaryContainer)
                    dos.writeInt(record.darkOnPrimaryContainer)
                    dos.writeInt(record.darkOnPrimary)
                }
            }
        } catch (_: Exception) {
            // 写入失败不阻塞主流程
        }
    }

    // 删除持久化文件（paletteStyle/colorSpec 变化时调用）
    fun clear(context: Context) = writeLock.withLock {
        File(context.filesDir, FILE_NAME).delete()
    }

    // 获取已持久化的种子色集合（用于预处理时跳过已计算项）
    // 按文件长度计算记录数，避免依赖文件头中不准确的 recordCount
    fun loadSeedColors(
        context: Context,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec
    ): Set<Int> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptySet()

        return try {
            DataInputStream(FileInputStream(file)).use { dis ->
                val version = dis.readInt()
                if (version != FILE_VERSION) return emptySet()

                val headerKey = dis.readUTF()
                if (headerKey != buildHeaderKey(paletteStyle, colorSpec)) return emptySet()

                dis.readInt()

                val headerKeyBytes = headerKey.toByteArray(Charsets.UTF_8).size
                val totalHeaderSize = 4 + 2 + headerKeyBytes + 4
                val remainingBytes = file.length() - totalHeaderSize
                val recordCount = (remainingBytes / RECORD_BYTES).toInt()

                val seeds = HashSet<Int>(recordCount)
                repeat(recordCount) {
                    seeds.add(dis.readInt())
                    // 跳过 4 个颜色数据
                    dis.skipBytes(COLOR_COUNT_PER_RECORD * 4)
                }
                seeds
            }
        } catch (_: Exception) {
            emptySet()
        }
    }
}
