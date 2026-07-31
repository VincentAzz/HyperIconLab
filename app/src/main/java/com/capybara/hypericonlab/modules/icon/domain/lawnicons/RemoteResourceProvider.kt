package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.InputStream

// 云端拉取版本资源提供者
// 所有资源（mapper / svgs / color_schemes / version）均在 filesDir/lawnicons_remote/<version>/ 下
// 由 LawniconsUpdateManager 下载解压后通过 LawniconsResourceManager 激活
class RemoteResourceProvider(
    context: Context,
    private val version: String
) : LawniconsResourceProvider {

    // 云端版本根目录：filesDir/lawnicons_remote/<version>/
    private val baseDir = File(context.filesDir, "${ProviderConstants.REMOTE_BASE_DIR}/$version")

    override fun openIconMapper(fileName: String): InputStream =
        File(baseDir, "${ProviderConstants.MAPPER_DIR}/$fileName").inputStream()

    override fun getSvgDir(): File? =
        File(baseDir, ProviderConstants.SVGS_DIR).takeIf { it.exists() }

    override fun openColorSchemes(fileName: String): InputStream =
        File(baseDir, "${ProviderConstants.COLOR_SCHEMES_DIR}/$fileName").inputStream()

    // 从 manifest.json 解析版本信息，svg 数从目录实际统计，失败时回退基本版本
    override fun getVersion(): LawniconsVersion {
        // svg 数始终从目录统计（manifest 中未记录）
        val actualSvgCount = getSvgDir()?.listFiles { f -> f.extension == "svg" }?.size ?: 0
        val manifestFile = File(baseDir, ProviderConstants.MANIFEST_FILE)
        if (!manifestFile.exists()) {
            return LawniconsVersion(
                version = version,
                source = ResourceSource.REMOTE,
                lawniconsCommit = "",
                generatedAt = "",
                svgCount = actualSvgCount,
                mapperCount = 0
            )
        }
        return try {
            val json = JSONObject(manifestFile.readText())
            val stats = json.optJSONObject(ProviderConstants.STATS_KEY)
            // manifest 的 total_icons 即 mapper item 数（唯一 package 数）
            LawniconsVersion(
                version = version,
                source = ResourceSource.REMOTE,
                lawniconsCommit = json.optString(ProviderConstants.COMMIT_KEY, ""),
                generatedAt = json.optString(ProviderConstants.GENERATED_AT_KEY, ""),
                svgCount = actualSvgCount,
                mapperCount = stats?.optInt(ProviderConstants.TOTAL_ICONS_KEY, 0) ?: 0
            )
        } catch (_: Exception) {
            LawniconsVersion(
                version = version,
                source = ResourceSource.REMOTE,
                lawniconsCommit = "",
                generatedAt = "",
                svgCount = actualSvgCount,
                mapperCount = 0
            )
        }
    }

    override fun getSourceType(): ResourceSource = ResourceSource.REMOTE

    private object ProviderConstants {
        const val REMOTE_BASE_DIR = "lawnicons_remote"
        const val MAPPER_DIR = "icon_mapper"
        const val COLOR_SCHEMES_DIR = "color_schemes"
        const val SVGS_DIR = "svgs"
        const val MANIFEST_FILE = "manifest.json"
        const val STATS_KEY = "stats"
        const val COMMIT_KEY = "lawnicons_commit"
        const val GENERATED_AT_KEY = "generated_at"
        const val TOTAL_ICONS_KEY = "total_icons"
    }
}
