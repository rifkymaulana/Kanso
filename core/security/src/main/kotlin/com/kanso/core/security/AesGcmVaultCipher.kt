package com.kanso.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AesGcmVaultCipher {

    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val IV_SIZE_BYTES = 12
    const val TAG_BITS = 128
    private val secureRandom = SecureRandom()

    fun generateTestKey(): SecretKey {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        return generator.generateKey()
    }

    fun encryptToBase64(plaintext: String, key: SecretKey): String {
        val iv = ByteArray(IV_SIZE_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encryptedBytes = cipher.doFinal(plaintext.encodeToByteArray())
        return packEncrypted(iv, encryptedBytes)
    }

    fun packEncrypted(iv: ByteArray, encryptedBytes: ByteArray): String {
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decryptFromBase64(encoded: String, key: SecretKey): String {
        val combined = Base64.getDecoder().decode(encoded)
        require(combined.size > IV_SIZE_BYTES) { "Invalid encrypted vault payload." }
        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val plainBytes = cipher.doFinal(ciphertext)
        return plainBytes.decodeToString()
    }
}
