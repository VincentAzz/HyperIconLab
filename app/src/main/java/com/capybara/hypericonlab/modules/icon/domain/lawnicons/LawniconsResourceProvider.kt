package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import java.io.File
import java.io.InputStream

// 资源来源类型：assets 出厂版本或 filesDir 云端拉取版本
enum class ResourceSource { ASSETS, REMOTE }

// lawnicons 资源版本信息
data class LawniconsVersion(
    val version: String,          // 版本号，如 "20260731"
    val source: ResourceSource,   // 来源类型
    val lawniconsCommit: String,  // lawnicons 上游 commit hash
    val generatedAt: String,      // 生成时间
    val svgCount: Int,            // svg 文件数（实际图标数）
    val mapperCount: Int          // mapper item 数（唯一 package 数）
) {
    companion object {
        // assets 出厂版本的默认版本信息（无 version.txt，使用硬编码）
        val ASSETS_DEFAULT = LawniconsVersion(
            version = "assets",
            source = ResourceSource.ASSETS,
            lawniconsCommit = "",
            generatedAt = "",
            svgCount = 0,
            mapperCount = 0
        )
    }
}

// 资源提供者接口：统一 mapper / svgs / color_schemes 的读取入口
// AssetsResourceProvider 读 assets + filesDir/lawnicons（回滚）
// RemoteResourceProvider 读 filesDir/lawnicons_remote/<version>（云端）
interface LawniconsResourceProvider {
    // 打开 icon_mapper 目录下的文件（icon_mapper.xml / icon_mapper_filtered.xml 等）
    fun openIconMapper(fileName: String): InputStream

    // 获取 svgs 目录（已解压到 filesDir），不存在返回 null
    fun getSvgDir(): File?

    // 打开 color_schemes 目录下的文件（app_color_schemes.xml）
    fun openColorSchemes(fileName: String): InputStream

    // 打开当前资源版本的稳定槽位映射；assets 版本未内置时返回 null
    fun openSlotMapping(): InputStream?

    // 打开当前版本的模板索引；尚未按需下载或 assets 不支持时返回 null
    fun openIconPackTemplateIndex(): InputStream?

    // 按集合打开未签名 APK 模板；模板尚未下载时返回 null
    fun openIconPackTemplate(iconSetId: String): InputStream?

    // 获取当前版本信息
    fun getVersion(): LawniconsVersion

    // 来源类型
    fun getSourceType(): ResourceSource
}
