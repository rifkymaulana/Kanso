package com.kanso.core.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupDecryptionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

object EncryptedBackupCrypto {

    private val MAGIC = byteArrayOf('K'.code.toByte(), 'N'.code.toByte(), 'S'.code.toByte(), 'O'.code.toByte())
    private const val VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val TAG_BYTES = TAG_BITS / 8
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    const val MIN_PASSPHRASE_LENGTH = 6

    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        require(passphrase.size >= MIN_PASSPHRASE_LENGTH) {
            "Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters."
        }

        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val key = deriveKey(passphrase, salt)

        val aad = byteArrayOf(MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3], VERSION)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)

        val buffer = ByteBuffer.allocate(MAGIC.size + 1 + SALT_BYTES + IV_BYTES + ciphertext.size)
        buffer.put(MAGIC)
        buffer.put(VERSION)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(ciphertext)
        return buffer.array()
    }

    fun decrypt(payload: ByteArray, passphrase: CharArray): ByteArray {
        val minHeaderBytes = MAGIC.size + 1 + SALT_BYTES + IV_BYTES + TAG_BYTES
        if (payload.size < minHeaderBytes) {
            throw BackupDecryptionException("Invalid or truncated backup file.")
        }

        val buffer = ByteBuffer.wrap(payload)
        val fileMagic = ByteArray(MAGIC.size).also { buffer.get(it) }
        if (!fileMagic.contentEquals(MAGIC)) {
            throw BackupDecryptionException("This file is not a valid Kanso encrypted backup.")
        }

        val version = buffer.get()
        if (version != VERSION) {
            throw BackupDecryptionException("Unsupported backup format version: $version.")
        }

        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }

        return try {
            val key = deriveKey(passphrase, salt)
            val aad = byteArrayOf(MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3], version)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad)
            cipher.doFinal(ciphertext)
        } catch (e: GeneralSecurityException) {
            throw BackupDecryptionException("Incorrect passphrase or corrupted backup file.", e)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val encoded = factory.generateSecret(spec).encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
