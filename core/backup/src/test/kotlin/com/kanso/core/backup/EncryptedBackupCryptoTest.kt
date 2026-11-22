package com.kanso.core.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EncryptedBackupCryptoTest {

    @Test
    fun `encrypt and decrypt round trip preserves exact bytes`() {
        val original = "Kanso personal backup payload with Unicode 🚀 Rp 50.000".encodeToByteArray()
        val encrypted = EncryptedBackupCrypto.encrypt(original, "my-secret-pass".toCharArray())
        val decrypted = EncryptedBackupCrypto.decrypt(encrypted, "my-secret-pass".toCharArray())

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `identical inputs produce distinct ciphertexts due to random salt and iv`() {
        val data = "Same plaintext".encodeToByteArray()
        val first = EncryptedBackupCrypto.encrypt(data, "strong-pass".toCharArray())
        val second = EncryptedBackupCrypto.encrypt(data, "strong-pass".toCharArray())

        assertFalse(first.contentEquals(second))
    }

    @Test(expected = BackupDecryptionException::class)
    fun `wrong passphrase throws BackupDecryptionException`() {
        val encrypted = EncryptedBackupCrypto.encrypt(
            "Sensitive notes".encodeToByteArray(),
            "correct-pass".toCharArray()
        )
        EncryptedBackupCrypto.decrypt(encrypted, "wrong-pass".toCharArray())
    }

    @Test(expected = BackupDecryptionException::class)
    fun `tampered ciphertext byte throws BackupDecryptionException`() {
        val encrypted = EncryptedBackupCrypto.encrypt(
            "Sensitive notes".encodeToByteArray(),
            "correct-pass".toCharArray()
        )
        val tampered = encrypted.copyOf()
        tampered[tampered.lastIndex] = (tampered[tampered.lastIndex].toInt() xor 0xFF).toByte()
        EncryptedBackupCrypto.decrypt(tampered, "correct-pass".toCharArray())
    }

    @Test(expected = BackupDecryptionException::class)
    fun `tampered header byte throws BackupDecryptionException`() {
        val encrypted = EncryptedBackupCrypto.encrypt(
            "Sensitive notes".encodeToByteArray(),
            "correct-pass".toCharArray()
        )
        val tampered = encrypted.copyOf()
        tampered[0] = 'X'.code.toByte()
        EncryptedBackupCrypto.decrypt(tampered, "correct-pass".toCharArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `short passphrase is rejected`() {
        EncryptedBackupCrypto.encrypt("data".encodeToByteArray(), "12345".toCharArray())
    }

    @Test(expected = BackupDecryptionException::class)
    fun `truncated payload throws BackupDecryptionException`() {
        EncryptedBackupCrypto.decrypt(byteArrayOf(1, 2, 3), "123456".toCharArray())
    }
}
