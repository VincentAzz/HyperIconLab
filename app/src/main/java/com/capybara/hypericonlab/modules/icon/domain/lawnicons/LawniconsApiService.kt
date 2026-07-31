package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// 云端 release 查询服务：从 GitHub Releases API 获取最新 lawnicons release
// 解析 release JSON + 下载 manifest.json 合并出 ReleaseInfo
// 未认证调用有 60 次/小时/IP 的速率限制，对单次检查更新足够
class LawniconsApiService {

    // 获取最新的 lawnicons release，失败返回 null
    suspend fun getLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val releasesJson =
                fetchJson("${ApiConstants.API_BASE}/releases?per_page=${ApiConstants.PER_PAGE}")
            val releases =
                releasesJson.optJSONArray(ApiConstants.RELEASES_KEY) ?: return@withContext null
            // 遍历找到 tag 以 lawnicons-v 开头的最新 release
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val tag = release.optString(ApiConstants.TAG_NAME_KEY, "")
                if (tag.startsWith(ApiConstants.TAG_PREFIX)) {
                    val version = tag.removePrefix(ApiConstants.TAG_PREFIX)
                    return@withContext parseRelease(version, release)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    // 解析单个 release：从 assets 找 zip 和 manifest.json，下载 manifest 合并信息
    private fun parseRelease(version: String, release: JSONObject): ReleaseInfo? {
        val assets = release.optJSONArray(ApiConstants.ASSETS_KEY) ?: return null
        var zipUrl = ""
        var zipSize = 0L
        var manifestUrl: String? = null

        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString(ApiConstants.ASSET_NAME_KEY, "")
            val url = asset.optString(ApiConstants.ASSET_URL_KEY, "")
            val size = asset.optLong(ApiConstants.ASSET_SIZE_KEY, 0L)
            when {
                name.endsWith(ApiConstants.ZIP_SUFFIX) -> {
                    zipUrl = url
                    zipSize = size
                }

                name == ApiConstants.MANIFEST_FILE -> manifestUrl = url
            }
        }

        // zip 必须存在，manifest 可选
        if (zipUrl.isEmpty()) return null

        // 下载并解析 manifest.json 获取 sha256 与图标统计
        val manifestInfo = manifestUrl?.let { parseManifest(it) }

        return ReleaseInfo(
            version = version,
            zipUrl = zipUrl,
            zipSizeBytes = zipSize,
            sha256 = manifestInfo?.sha256 ?: "",
            lawniconsCommit = manifestInfo?.commit ?: "",
            generatedAt = manifestInfo?.generatedAt ?: "",
            totalIcons = manifestInfo?.totalIcons ?: 0,
            addedIcons = manifestInfo?.added ?: 0,
            removedIcons = manifestInfo?.removed ?: 0,
            modifiedIcons = manifestInfo?.modified ?: 0
        )
    }

    // 下载 manifest.json 并解析关键字段
    private fun parseManifest(url: String): ManifestInfo? {
        return try {
            val json = fetchJson(url)
            val pkg = json.optJSONObject(ApiConstants.PACKAGE_KEY)
            val stats = json.optJSONObject(ApiConstants.STATS_KEY)
            ManifestInfo(
                sha256 = pkg?.optString(ApiConstants.SHA256_KEY, "") ?: "",
                commit = json.optString(ApiConstants.COMMIT_KEY, ""),
                generatedAt = json.optString(ApiConstants.GENERATED_AT_KEY, ""),
                totalIcons = stats?.optInt(ApiConstants.TOTAL_ICONS_KEY, 0) ?: 0,
                added = stats?.optInt(ApiConstants.ADDED_KEY, 0) ?: 0,
                removed = stats?.optInt(ApiConstants.REMOVED_KEY, 0) ?: 0,
                modified = stats?.optInt(ApiConstants.MODIFIED_KEY, 0) ?: 0
            )
        } catch (_: Exception) {
            null
        }
    }

    // 发起 GET 请求并解析 JSON 响应
    private fun fetchJson(urlStr: String): JSONObject {
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = ApiConstants.METHOD_GET
            connectTimeout = ApiConstants.CONNECT_TIMEOUT_MS
            readTimeout = ApiConstants.READ_TIMEOUT_MS
            setRequestProperty(ApiConstants.HEADER_USER_AGENT, ApiConstants.USER_AGENT)
            setRequestProperty(ApiConstants.HEADER_ACCEPT, ApiConstants.ACCEPT_JSON)
        }
        return connection.inputStream.use { stream ->
            val text = BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
            JSONObject(text)
        }
    }

    // manifest 解析结果中间结构
    private data class ManifestInfo(
        val sha256: String,
        val commit: String,
        val generatedAt: String,
        val totalIcons: Int,
        val added: Int,
        val removed: Int,
        val modified: Int
    )

    private object ApiConstants {
        // GitHub API 基础地址
        const val API_BASE = "https://api.github.com/repos/VincentAzz/HyperIconLab"
        const val PER_PAGE = 10

        // release tag 前缀
        const val TAG_PREFIX = "lawnicons-v"

        // HTTP 配置
        const val METHOD_GET = "GET"
        const val CONNECT_TIMEOUT_MS = 15000
        const val READ_TIMEOUT_MS = 60000
        const val USER_AGENT = "HyperIconLab-Android"
        const val ACCEPT_JSON = "application/vnd.github+json"
        const val HEADER_USER_AGENT = "User-Agent"
        const val HEADER_ACCEPT = "Accept"

        // JSON 字段名
        const val RELEASES_KEY = "releases"
        const val TAG_NAME_KEY = "tag_name"
        const val ASSETS_KEY = "assets"
        const val ASSET_NAME_KEY = "name"
        const val ASSET_URL_KEY = "browser_download_url"
        const val ASSET_SIZE_KEY = "size"
        const val ZIP_SUFFIX = ".zip"
        const val MANIFEST_FILE = "manifest.json"
        const val PACKAGE_KEY = "package"
        const val STATS_KEY = "stats"
        const val SHA256_KEY = "sha256"
        const val COMMIT_KEY = "lawnicons_commit"
        const val GENERATED_AT_KEY = "generated_at"
        const val TOTAL_ICONS_KEY = "total_icons"
        const val ADDED_KEY = "added"
        const val REMOVED_KEY = "removed"
        const val MODIFIED_KEY = "modified"
    }
}
