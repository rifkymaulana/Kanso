package com.kanso.core.backup

import com.kanso.core.model.KansoBackupSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(snapshot: KansoBackupSnapshot): ByteArray {
        return json.encodeToString(snapshot).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): KansoBackupSnapshot {
        val snapshot = try {
            json.decodeFromString<KansoBackupSnapshot>(bytes.decodeToString())
        } catch (e: Exception) {
            throw BackupDecryptionException("Corrupted backup data.", e)
        }
        if (snapshot.schemaVersion > KansoBackupSnapshot.CURRENT_SCHEMA_VERSION) {
            throw BackupDecryptionException(
                "Backup schema v${snapshot.schemaVersion} is newer than this app supports."
            )
        }
        return snapshot
    }
}
