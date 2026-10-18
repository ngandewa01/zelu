package com.nagesh.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

// The key derivation is written by hand, so it is checked against published PBKDF2-HMAC-SHA256 test vectors.
class BackupCryptoTest {
    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    @Test
    fun keyDerivationMatchesKnownVectors() {
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b", hex(BackupCrypto.pbkdf2("password".toCharArray(), "salt".toByteArray(), 1, 32)))
        assertEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134af7ad98c1b458ce3f", hex(BackupCrypto.pbkdf2("password".toCharArray(), "salt".toByteArray(), 4096, 40)))
    }
}
