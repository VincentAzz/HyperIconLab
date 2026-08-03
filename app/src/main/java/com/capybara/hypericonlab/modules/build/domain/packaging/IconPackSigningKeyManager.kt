package com.capybara.hypericonlab.modules.build.domain.packaging

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import java.util.Date
import javax.security.auth.x500.X500Principal

data class IconPackSigningIdentity(
    val privateKey: PrivateKey,
    val certificate: X509Certificate
)

// 在 AndroidKeyStore 中创建并复用用户专属图标包签名密钥，私钥不可导出。
class IconPackSigningKeyManager {
    @Synchronized
    fun getOrCreate(): IconPackSigningIdentity {
        val keyStore = KeyStore.getInstance(SigningKeyConfig.KEYSTORE_PROVIDER).apply { load(null) }
        readIdentity(keyStore)?.let { return it }
        generateKeyPair()
        return readIdentity(keyStore)
            ?: error("AndroidKeyStore 已生成密钥但无法读取签名身份")
    }

    private fun readIdentity(keyStore: KeyStore): IconPackSigningIdentity? {
        val privateKey = keyStore.getKey(SigningKeyConfig.KEY_ALIAS, null) as? PrivateKey
            ?: return null
        val certificate = keyStore.getCertificate(SigningKeyConfig.KEY_ALIAS) as? X509Certificate
            ?: return null
        return IconPackSigningIdentity(privateKey, certificate)
    }

    private fun generateKeyPair() {
        val now = Date()
        val notBefore = Date(now.time - SigningKeyConfig.CERTIFICATE_CLOCK_SKEW_MS)
        val notAfter = Calendar.getInstance().apply {
            time = now
            add(Calendar.YEAR, SigningKeyConfig.CERTIFICATE_VALIDITY_YEARS)
        }.time
        val serialNumber = BigInteger(SigningKeyConfig.SERIAL_BITS, SecureRandom()).abs()
        val parameters = KeyGenParameterSpec.Builder(
            SigningKeyConfig.KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(SigningKeyConfig.RSA_KEY_SIZE)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal(SigningKeyConfig.CERTIFICATE_SUBJECT))
            .setCertificateSerialNumber(serialNumber)
            .setCertificateNotBefore(notBefore)
            .setCertificateNotAfter(notAfter)
            .setUserAuthenticationRequired(false)
            .build()
        KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            SigningKeyConfig.KEYSTORE_PROVIDER
        ).apply {
            initialize(parameters)
            generateKeyPair()
        }
    }

    private object SigningKeyConfig {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "hypericonlab_iconpack_signing_v1"
        const val RSA_KEY_SIZE = 3072
        const val SERIAL_BITS = 128
        const val CERTIFICATE_VALIDITY_YEARS = 30
        const val CERTIFICATE_CLOCK_SKEW_MS = 24L * 60L * 60L * 1000L
        const val CERTIFICATE_SUBJECT = "CN=HyperIconLab User Icon Pack,O=HyperIconLab"
    }
}
