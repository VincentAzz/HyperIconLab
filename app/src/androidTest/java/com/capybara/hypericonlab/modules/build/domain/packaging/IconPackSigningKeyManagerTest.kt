package com.capybara.hypericonlab.modules.build.domain.packaging

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.Signature

@RunWith(AndroidJUnit4::class)
class IconPackSigningKeyManagerTest {
    @Test
    fun getOrCreate_reusesNonExportableSigningIdentity() {
        val manager = IconPackSigningKeyManager()
        val first = manager.getOrCreate()
        val second = manager.getOrCreate()
        val payload = "HyperIconLab icon pack signing test".toByteArray()
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(first.privateKey)
            update(payload)
            sign()
        }
        val verified = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(first.certificate.publicKey)
            update(payload)
            verify(signature)
        }

        assertNull(first.privateKey.encoded)
        assertArrayEquals(first.certificate.encoded, second.certificate.encoded)
        assertTrue(verified)
    }

    private companion object {
        const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    }
}
