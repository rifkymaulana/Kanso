package com.kanso.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import javax.crypto.AEADBadTagException

class AesGcmVaultCipherTest {

    @Test
    fun `encrypt and decrypt round trip preserves plaintext`() {
        val key = AesGcmVaultCipher.generateTestKey()
        val secret = "Recovery codes:\n1) ABCD-1234\n2) EFGH-5678 🔐"

        val encrypted = AesGcmVaultCipher.encryptToBase64(secret, key)
        val decrypted = AesGcmVaultCipher.decryptFromBase64(encrypted, key)

        assertEquals(secret, decrypted)
    }

    @Test
    fun `identical plaintexts produce different ciphertexts`() {
        val key = AesGcmVaultCipher.generateTestKey()
        val first = AesGcmVaultCipher.encryptToBase64("same secret", key)
        val second = AesGcmVaultCipher.encryptToBase64("same secret", key)

        assertNotEquals(first, second)
    }

    @Test(expected = AEADBadTagException::class)
    fun `wrong key fails authentication tag check`() {
        val keyA = AesGcmVaultCipher.generateTestKey()
        val keyB = AesGcmVaultCipher.generateTestKey()
        val encrypted = AesGcmVaultCipher.encryptToBase64("top secret", keyA)

        AesGcmVaultCipher.decryptFromBase64(encrypted, keyB)
    }

    @Test(expected = AEADBadTagException::class)
    fun `tampered ciphertext fails decryption`() {
        val key = AesGcmVaultCipher.generateTestKey()
        val encrypted = AesGcmVaultCipher.encryptToBase64("top secret", key)
        val raw = java.util.Base64.getDecoder().decode(encrypted)
        raw[raw.lastIndex] = (raw[raw.lastIndex].toInt() xor 0xFF).toByte()
        val tampered = java.util.Base64.getEncoder().encodeToString(raw)

        AesGcmVaultCipher.decryptFromBase64(tampered, key)
    }
}
