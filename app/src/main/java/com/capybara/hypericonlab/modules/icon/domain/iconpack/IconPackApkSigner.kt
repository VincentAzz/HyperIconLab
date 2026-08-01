package com.capybara.hypericonlab.modules.icon.domain.iconpack

import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import com.android.apksig.KeyConfig
import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.jar.JarFile

data class IconPackSigningResult(
    val outputFile: File,
    val certificateSha256: String,
    val verifiedV1: Boolean,
    val verifiedV2: Boolean
)

// 使用 apksig 生成 v1 + v2 签名，并在切换最终产物前完成 ApkVerifier 自检。
class IconPackApkSigner {
    fun signAndVerify(
        unsignedApk: File,
        outputApk: File,
        identity: IconPackSigningIdentity
    ): IconPackSigningResult {
        require(unsignedApk.isFile) { "待签名 APK 不存在" }
        outputApk.parentFile?.mkdirs()
        val temporaryOutput = File(outputApk.parentFile, "${outputApk.name}.signing")
        temporaryOutput.delete()

        return try {
            val signerConfig = ApkSigner.SignerConfig.Builder(
                SigningConfig.SIGNER_NAME,
                KeyConfig.Jca(identity.privateKey),
                listOf(identity.certificate)
            ).build()
            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(unsignedApk)
                .setOutputApk(temporaryOutput)
                // 使用 23 强制 apksig 同时生成 v1；不会修改 Manifest 的实际 minSdk 26。
                .setMinSdkVersion(SigningConfig.SIGNING_MIN_SDK)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(false)
                .setV4SigningEnabled(false)
                .setAlignmentPreserved(true)
                .setOtherSignersSignaturesPreserved(false)
                .build()
                .sign()

            val verification = ApkVerifier.Builder(temporaryOutput)
                .setMinCheckedPlatformVersion(SigningConfig.MIN_SDK)
                .build()
                .verify()
            require(verification.isVerified) {
                "APK 签名自检失败: ${verification.errors.joinToString()}"
            }
            require(verification.isVerifiedUsingV2Scheme) { "APK 缺少有效 v2 签名" }
            verifyV1JarSignature(temporaryOutput, identity.certificate)
            val actualCertificate = verification.signerCertificates.singleOrNull()
                ?: error("APK 签名证书数量不符合预期")
            require(actualCertificate.encoded.contentEquals(identity.certificate.encoded)) {
                "APK 签名证书与用户密钥不一致"
            }

            if (outputApk.exists() && !outputApk.delete()) error("无法覆盖旧签名 APK")
            if (!temporaryOutput.renameTo(outputApk)) error("无法切换签名 APK 临时产物")
            IconPackSigningResult(
                outputFile = outputApk,
                certificateSha256 = sha256(identity.certificate.encoded),
                verifiedV1 = true,
                verifiedV2 = true
            )
        } catch (e: Exception) {
            temporaryOutput.delete()
            throw e
        }
    }

    private fun verifyV1JarSignature(apkFile: File, expectedCertificate: X509Certificate) {
        var signedEntryCount = 0
        JarFile(apkFile, true).use { jar ->
            jar.entries().asSequence()
                .filterNot { it.isDirectory || it.name.startsWith(SigningConfig.META_INF_DIR) }
                .forEach { entry ->
                    // 必须完整读取条目，JarFile 才会执行摘要和签名校验。
                    jar.getInputStream(entry).use { input ->
                        val buffer = ByteArray(SigningConfig.BUFFER_SIZE)
                        while (input.read(buffer) != -1) Unit
                    }
                    val certificates = entry.certificates.orEmpty()
                    require(certificates.any { certificate ->
                        certificate.encoded.contentEquals(expectedCertificate.encoded)
                    }) { "APK v1 签名条目证书不一致: ${entry.name}" }
                    signedEntryCount++
                }
        }
        require(signedEntryCount > 0) { "APK 缺少有效 v1 签名条目" }
    }

    private fun sha256(content: ByteArray): String =
        MessageDigest.getInstance(SigningConfig.SHA256_ALGORITHM)
            .digest(content)
            .joinToString("") { SigningConfig.HEX_FORMAT.format(it) }

    private object SigningConfig {
        const val SIGNER_NAME = "HYPERICONLAB"
        const val MIN_SDK = 26
        const val SIGNING_MIN_SDK = 23
        const val BUFFER_SIZE = 8192
        const val META_INF_DIR = "META-INF/"
        const val SHA256_ALGORITHM = "SHA-256"
        const val HEX_FORMAT = "%02x"
    }
}
