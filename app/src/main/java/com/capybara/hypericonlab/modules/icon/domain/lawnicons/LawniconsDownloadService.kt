package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import android.content.Context
import com.capybara.hypericonlab.core.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest

// 云端资源下载服务：负责下载 zip、校验 sha256、解压到版本目录
// 下载文件存于 cacheDir，失败时自动清理缓存文件，避免污染 filesDir
// 失败时抛 LawniconsUpdateException（含 FailureReason），由 UpdateManager 捕获映射文案
class LawniconsDownloadService(
    private val context: Context
) {

    // 下载 zip 到 cacheDir，返回缓存文件
    // onProgress 回调 0~1 的下载进度
    // 失败抛 LawniconsUpdateException：403→RATE_LIMITED，超时→TIMEOUT，断网→NETWORK_ERROR，其他非 200→HTTP_ERROR
    suspend fun download(
        url: String,
        expectedSize: Long,
        cacheFileName: String = DownloadConstants.BUNDLE_CACHE_FILE_NAME,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        require(cacheFileName == File(cacheFileName).name) { "缓存文件名不能包含路径" }
        val cacheFile = File(context.cacheDir, cacheFileName)
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = DownloadConstants.METHOD_GET
                connectTimeout = DownloadConstants.CONNECT_TIMEOUT_MS
                readTimeout = DownloadConstants.READ_TIMEOUT_MS
                setRequestProperty(
                    DownloadConstants.HEADER_USER_AGENT,
                    DownloadConstants.USER_AGENT
                )
                setRequestProperty(DownloadConstants.HEADER_ACCEPT, DownloadConstants.ACCEPT_BINARY)
            }
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != DownloadConstants.HTTP_OK) {
                val reason = if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    FailureReason.RATE_LIMITED
                } else {
                    FailureReason.HTTP_ERROR
                }
                throw LawniconsUpdateException(reason, "HTTP $responseCode")
            }
            // 优先用 expectedSize，无则用响应头 content-length
            val total = expectedSize.takeIf { it > 0 }
                ?: connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            cacheFile.outputStream().use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(DownloadConstants.BUFFER_SIZE)
                    var read: Int
                    var downloaded = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            onProgress((downloaded.toFloat() / total).coerceIn(0f, 0.99f))
                        }
                    }
                    output.flush()
                }
            }
            onProgress(1.0f)
            cacheFile
        } catch (e: LawniconsUpdateException) {
            // 已分类异常直接向上抛
            cacheFile.delete()
            throw e
        } catch (e: SocketTimeoutException) {
            cacheFile.delete()
            throw LawniconsUpdateException(FailureReason.TIMEOUT, e.message, e)
        } catch (e: UnknownHostException) {
            cacheFile.delete()
            throw LawniconsUpdateException(FailureReason.NETWORK_ERROR, e.message, e)
        } catch (e: IOException) {
            cacheFile.delete()
            throw LawniconsUpdateException(FailureReason.NETWORK_ERROR, e.message, e)
        } catch (e: Exception) {
            cacheFile.delete()
            throw LawniconsUpdateException(FailureReason.UNKNOWN, e.message, e)
        }
    }

    // 校验文件 sha256，expected 为空时跳过校验直接通过
    // 返回 false 表示校验不匹配，由调用方决定抛 CORRUPTED 异常
    fun verifySha256(file: File, expected: String): Boolean {
        if (expected.isBlank()) return true
        val actual = MessageDigest.getInstance(DownloadConstants.SHA256_ALGORITHM)
            .digest(file.readBytes())
            .joinToString("") { DownloadConstants.HEX_FORMAT.format(it) }
        return actual.equals(expected, ignoreCase = true)
    }

    // 解压 zip 到目标目录
    // onProgress 回调 0~1 的解压进度
    // 失败抛 LawniconsUpdateException(EXTRACT_FAILED)，并清理半成品目录
    suspend fun extract(
        zipFile: File,
        targetDir: File,
        onProgress: (Float) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        try {
            targetDir.mkdirs()
            ZipUtils.unzip(zipFile.inputStream(), targetDir, onProgress)
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            throw LawniconsUpdateException(FailureReason.EXTRACT_FAILED, e.message, e)
        }
    }

    // 清理下载缓存文件
    fun cleanupCache(cacheFileName: String = DownloadConstants.BUNDLE_CACHE_FILE_NAME) {
        File(context.cacheDir, cacheFileName).delete()
    }

    private object DownloadConstants {
        const val BUNDLE_CACHE_FILE_NAME = "lawnicons_update.zip"
        const val METHOD_GET = "GET"
        const val CONNECT_TIMEOUT_MS = 15000
        const val READ_TIMEOUT_MS = 60000
        const val USER_AGENT = "HyperIconLab-Android"
        const val ACCEPT_BINARY = "application/octet-stream"
        const val HEADER_USER_AGENT = "User-Agent"
        const val HEADER_ACCEPT = "Accept"
        const val HTTP_OK = 200
        const val BUFFER_SIZE = 8192
        const val SHA256_ALGORITHM = "SHA-256"
        const val HEX_FORMAT = "%02x"
    }
}
