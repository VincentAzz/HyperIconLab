package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import android.content.Context
import com.capybara.hypericonlab.core.AppVersion
import com.capybara.hypericonlab.core.utils.ZipUtils
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.mapper.IconMapperProcessor
import java.io.File
import java.io.InputStream

// assets 出厂版本资源提供者
// mapper / color_schemes 从 assets 直读，svgs 从 filesDir/lawnicons 解压目录读取
// 当无云端版本时作为回滚使用
class AssetsResourceProvider(
    private val context: Context
) : LawniconsResourceProvider {

    override fun openIconMapper(fileName: String): InputStream =
        context.assets.open("${ProviderConstants.MAPPER_DIR}/$fileName")

    override fun getSvgDir(): File? {
        val lawniconsBase = File(context.filesDir, ProviderConstants.LAWNICONS_DIR)
        return ZipUtils.findDirRecursive(lawniconsBase, ProviderConstants.SVGS_DIR)
    }

    override fun openColorSchemes(fileName: String): InputStream =
        context.assets.open("${ProviderConstants.COLOR_SCHEMES_DIR}/$fileName")

    override fun openSlotMapping(): InputStream? = null

    override fun openIconPackTemplateIndex(): InputStream? = null

    override fun openIconPackTemplate(iconSetId: String): InputStream? = null

    // assets 版本的版本信息：svg 数从目录统计，mapper 数从 icon_mapper.xml 解析
    override fun getVersion(): LawniconsVersion {
        val svgCount = getSvgDir()?.listFiles { f -> f.extension == "svg" }?.size ?: 0
        val mapperCount = try {
            openIconMapper(ProviderConstants.FULL_MAPPER_FILE)
                .use { IconMapperProcessor.parseIconMapper(it).size }
        } catch (_: Exception) {
            0
        }
        return LawniconsVersion(
            version = AppVersion.LAWNICONS_VERSION.substringBefore("-"),
            source = ResourceSource.ASSETS,
            lawniconsCommit = AppVersion.LAWNICONS_VERSION.substringAfter("-", ""),
            generatedAt = "",
            svgCount = svgCount,
            mapperCount = mapperCount
        )
    }

    override fun getSourceType(): ResourceSource = ResourceSource.ASSETS

    private object ProviderConstants {
        const val MAPPER_DIR = "icon_mapper"
        const val COLOR_SCHEMES_DIR = "color_schemes"
        const val LAWNICONS_DIR = "lawnicons"
        const val SVGS_DIR = "svgs"
        const val FULL_MAPPER_FILE = "icon_mapper.xml"
    }
}
