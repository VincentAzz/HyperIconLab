package com.capybara.hypericonlab.modules.build.domain.packaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

// 使用外部测试证书验证真实 APK 签名；普通 CI 未配置样本时自动跳过。
class IconPackApkSignerLocalIntegrationTest {
    @Test
    fun signAndVerify_producesV1AndV2SignedApk() {
        val unsignedApk = System.getenv(TestConfig.UNSIGNED_APK_ENV)?.let(::File)
        val outputApk = System.getenv(TestConfig.OUTPUT_APK_ENV)?.let(::File)
        val keyStoreFile = System.getenv(TestConfig.KEYSTORE_ENV)?.let(::File)
        val password = System.getenv(TestConfig.KEYSTORE_PASSWORD_ENV)?.toCharArray()
        assumeTrue(
            unsignedApk?.isFile == true && outputApk != null &&
                    keyStoreFile?.isFile == true && password != null
        )

        val keyStore = KeyStore.getInstance(TestConfig.KEYSTORE_TYPE).apply {
            keyStoreFile!!.inputStream().use { load(it, password) }
        }
        val alias = keyStore.aliases().toList().single()
        val identity = IconPackSigningIdentity(
            privateKey = keyStore.getKey(alias, password) as PrivateKey,
            certificate = keyStore.getCertificate(alias) as X509Certificate
        )

        val result = IconPackApkSigner().signAndVerify(
            unsignedApk = unsignedApk!!,
            outputApk = outputApk!!,
            identity = identity
        )

        assertTrue(result.verifiedV1)
        assertTrue(result.verifiedV2)
        assertEquals(64, result.certificateSha256.length)
    }

    private object TestConfig {
        const val UNSIGNED_APK_ENV = "ICONPACK_SIGNING_TEST_UNSIGNED_APK"
        const val OUTPUT_APK_ENV = "ICONPACK_SIGNING_TEST_OUTPUT_APK"
        const val KEYSTORE_ENV = "ICONPACK_SIGNING_TEST_KEYSTORE"
        const val KEYSTORE_PASSWORD_ENV = "ICONPACK_SIGNING_TEST_KEYSTORE_PASSWORD"
        const val KEYSTORE_TYPE = "PKCS12"
    }
}
