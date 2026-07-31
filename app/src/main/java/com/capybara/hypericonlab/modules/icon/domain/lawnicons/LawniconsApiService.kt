package com.capybara.hypericonlab.modules.icon.domain.lawnicons


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

// 云端 release 查询服务：从 GitHub Releases API 获取最新 lawnicons release
// 解析 release JSON + 下载 manifest.json 合并出 ReleaseInfo
// 未认证调用有 60 次/小时/IP 的速率限制，对单次检查更新足够
// 失败时抛 LawniconsUpdateException（含 FailureReason），由 UpdateManager 捕获映射
class LawniconsApiService {

    // 获取最新的 lawnicons release
    // proxyPrefix 非空时仅对 github.com 资源下载 URL（manifest）加前缀，API 调用本身不走代理
    // 失败抛 LawniconsUpdateException；无匹配 release 时返回 null（非错误）
    suspend fun getLatestRelease(proxyPrefix: String = ""): ReleaseInfo? =
        withContext(Dispatchers.IO) {
            try {
                val url = "${ApiConstants.API_BASE}/releases?per_page=${ApiConstants.PER_PAGE}"
                Timber.tag("LawniconsApiService").d("Fetching releases from: $url")
                val responseText = fetchRaw(url)
                val releases = JSONArray(responseText)
                Timber.tag("LawniconsApiService").d("Found ${releases.length()} releases")
                // 遍历找到 tag 以 lawnicons-v 开头的最新 release
                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val tag = release.optString(ApiConstants.TAG_NAME_KEY, "")
                    Timber.tag("LawniconsApiService").d("Checking release: $tag")
                    if (tag.startsWith(ApiConstants.TAG_PREFIX)) {
                        val version = tag.removePrefix(ApiConstants.TAG_PREFIX)
                        val info = parseRelease(version, release, proxyPrefix)
                        if (info != null) {
                            Timber.tag("LawniconsApiService")
                                .d("Successfully matched and parsed release: $tag")
                            return@withContext info
                        } else {
                            Timber.tag("LawniconsApiService")
                                .w("Matched tag $tag but failed to parse release details")
                        }
                    }
                }
                Timber.tag("LawniconsApiService")
                    .w("No release matching prefix ${ApiConstants.TAG_PREFIX} found")
                null
            } catch (e: LawniconsUpdateException) {
                // 已分类的异常直接向上抛
                throw e
            } catch (e: SocketTimeoutException) {
                throw LawniconsUpdateException(FailureReason.TIMEOUT, e.message, e)
            } catch (e: UnknownHostException) {
                throw LawniconsUpdateException(FailureReason.NETWORK_ERROR, e.message, e)
            } catch (e: org.json.JSONException) {
                throw LawniconsUpdateException(FailureReason.PARSE_ERROR, e.message, e)
            } catch (e: Exception) {
                // 兜底：其他 IO/运行时异常归为网络错误
                throw LawniconsUpdateException(
                    if (e is java.io.IOException) FailureReason.NETWORK_ERROR else FailureReason.UNKNOWN,
                    e.message, e
                )
            }
        }


    // 解析单个 release：从 assets 找 zip 和 manifest.json，下载 manifest 合并信息
    // proxyPrefix 非空时对 manifest 下载 URL 加前缀加速
    private fun parseRelease(
        version: String,
        release: JSONObject,
        proxyPrefix: String
    ): ReleaseInfo? {
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
                    // zip 下载 URL 也加代理前缀，供 DownloadService 直接使用
                    zipUrl = applyProxy(url, proxyPrefix)
                    zipSize = size
                }

                name == ApiConstants.MANIFEST_FILE -> manifestUrl = applyProxy(url, proxyPrefix)
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

    // 对 github.com 的 URL 加代理前缀，非 github URL 或空前缀原样返回
    private fun applyProxy(url: String, proxyPrefix: String): String {
        if (proxyPrefix.isBlank() || !url.contains(ApiConstants.GITHUB_HOST)) return url
        return proxyPrefix + url
    }

    // 下载 manifest.json 并解析关键字段
    // 解析失败抛 LawniconsUpdateException(PARSE_ERROR)
    private fun parseManifest(url: String): ManifestInfo? {
        return try {
            val text = fetchRaw(url)
            val json = JSONObject(text)
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
        } catch (e: LawniconsUpdateException) {
            // fetchRaw 抛出的已分类异常，向上传递
            throw e
        } catch (e: org.json.JSONException) {
            throw LawniconsUpdateException(FailureReason.PARSE_ERROR, e.message, e)
        } catch (_: Exception) {
            // manifest 下载失败不阻断主流程（sha256 等字段降级为空）
            null
        }
    }

    // 发起 GET 请求并返回原始响应文本
    // HTTP 403 → RATE_LIMITED；其他非 200 → HTTP_ERROR
    private fun fetchRaw(urlStr: String): String {
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = ApiConstants.METHOD_GET
            connectTimeout = ApiConstants.CONNECT_TIMEOUT_MS
            readTimeout = ApiConstants.READ_TIMEOUT_MS
            setRequestProperty(ApiConstants.HEADER_USER_AGENT, ApiConstants.USER_AGENT)
            setRequestProperty(ApiConstants.HEADER_ACCEPT, ApiConstants.ACCEPT_JSON)
        }
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorText = connection.errorStream?.use { it.bufferedReader().readText() } ?: ""
            Timber.tag("LawniconsApiService").e("HTTP error $responseCode for $urlStr: $errorText")
            val reason = if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                FailureReason.RATE_LIMITED
            } else {
                FailureReason.HTTP_ERROR
            }
            throw LawniconsUpdateException(reason, "HTTP $responseCode")
        }
        return connection.inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
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

        // github.com 主机标识，用于判断是否需要加代理前缀
        const val GITHUB_HOST = "github.com"
        const val PER_PAGE = 10

        // release tag 前缀
        const val TAG_PREFIX = "lawnicons-v"

        // HTTP 配置
        const val METHOD_GET = "GET"
        const val CONNECT_TIMEOUT_MS = 15000
        const val READ_TIMEOUT_MS = 60000

        // 使用更标准且具有辨识度的 User-Agent，避免被 GitHub 拦截
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 HyperIconLab/1.0"
        const val ACCEPT_JSON = "application/vnd.github+json"
        const val HEADER_USER_AGENT = "User-Agent"

        const val HEADER_ACCEPT = "Accept"

        // JSON 字段名
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
