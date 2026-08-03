package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import com.capybara.hypericonlab.core.utils.ZipUtils
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.mapper.IconMapperProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ManageResourcesUseCase(private val context: Context) {

    private val filesDir = context.filesDir
    private val lawniconsBase = File(filesDir, "lawnicons")

    // 手动生成 mapper 时的输出目录（assets 中已内置 mapper，此目录仅供 generateMapper 使用）
    private val mapperBase = File(filesDir, "icon_mapper")

    suspend fun performUnzip(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        // 仅解压 lawnicons.zip（svg 资源仍需落盘供管线读取），mapper 已直接打包在 assets 中
        ZipUtils.unzip(context.assets.open("lawnicons.zip"), lawniconsBase) { p ->
            onProgress(p)
        }
    }

    /**
     * 手动生成 mapper：从 lawnicons 解压目录的 appfilter.xml 与 assets 中的 alt mapper 合并，
     * 输出到 filesDir/icon_mapper/icon_mapper.xml。
     * 保留供"手动生成"入口使用，runPipeline 不会读取此产物（直接走 assets）。
     */
    suspend fun generateMapper(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val appfilter = ZipUtils.findFileRecursive(lawniconsBase, "appfilter.xml")
            // alt mapper 直接从 assets 读取并写入临时文件，供 convertIconMapper 使用
            val altMapper = File(mapperBase, MapperAssets.ALT_MAPPER_FILENAME)
            try {
                context.assets.open(MapperAssets.ASSET_ALT_MAPPER_PATH).use { input ->
                    altMapper.parentFile?.mkdirs()
                    altMapper.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                // alt mapper 不存在时按无合并处理
                altMapper.delete()
            }
            val target = File(mapperBase, MapperAssets.OUTPUT_MAPPER_FILENAME)

            if (appfilter != null) {
                target.parentFile?.mkdirs()
                IconMapperProcessor.convertIconMapper(
                    appfilter,
                    target,
                    if (altMapper.exists()) altMapper else null
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception("未找到 appfilter.xml"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 资源路径相关常量集中声明，便于调参
    private object MapperAssets {
        // assets 中 alt mapper 的相对路径
        const val ASSET_ALT_MAPPER_PATH = "icon_mapper/icon_mapper_alt.xml"

        // alt mapper 临时落盘文件名
        const val ALT_MAPPER_FILENAME = "icon_mapper_alt.xml"

        // 手动生成 mapper 的输出文件名（相对于 mapperBase）
        const val OUTPUT_MAPPER_FILENAME = "icon_mapper/icon_mapper.xml"
    }
}
