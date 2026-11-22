package com.kanso.core.backup.drive

enum class DriveSyncForceMode {
    SMART,
    FORCE_UPLOAD,
    FORCE_DOWNLOAD
}

sealed interface DriveSyncDecision {
    data object UploadInitial : DriveSyncDecision
    data class PushLocal(val remoteFileId: String) : DriveSyncDecision
    data class PullRemote(val remoteFile: DriveRemoteFile) : DriveSyncDecision
    data class UpToDate(val remoteFile: DriveRemoteFile) : DriveSyncDecision
    data class Conflict(val remoteFile: DriveRemoteFile, val localItemCount: Int) : DriveSyncDecision
    data object NoRemoteBackup : DriveSyncDecision
}

object DriveSyncPlanner {

    fun decide(
        localContentHash: String,
        localItemCount: Int,
        lastSyncedContentHash: String?,
        lastSyncedRemoteModifiedMillis: Long?,
        remoteFile: DriveRemoteFile?,
        forceMode: DriveSyncForceMode = DriveSyncForceMode.SMART
    ): DriveSyncDecision {
        return when (forceMode) {
            DriveSyncForceMode.FORCE_UPLOAD -> {
                if (remoteFile == null) {
                    DriveSyncDecision.UploadInitial
                } else {
                    DriveSyncDecision.PushLocal(remoteFile.id)
                }
            }

            DriveSyncForceMode.FORCE_DOWNLOAD -> {
                if (remoteFile == null) {
                    DriveSyncDecision.NoRemoteBackup
                } else {
                    DriveSyncDecision.PullRemote(remoteFile)
                }
            }

            DriveSyncForceMode.SMART -> decideSmart(
                localContentHash = localContentHash,
                localItemCount = localItemCount,
                lastSyncedContentHash = lastSyncedContentHash,
                lastSyncedRemoteModifiedMillis = lastSyncedRemoteModifiedMillis,
                remoteFile = remoteFile
            )
        }
    }

    private fun decideSmart(
        localContentHash: String,
        localItemCount: Int,
        lastSyncedContentHash: String?,
        lastSyncedRemoteModifiedMillis: Long?,
        remoteFile: DriveRemoteFile?
    ): DriveSyncDecision {
        if (remoteFile == null) {
            return DriveSyncDecision.UploadInitial
        }

        val remoteHash = remoteFile.contentHash
        if (!remoteHash.isNullOrBlank() && remoteHash == localContentHash) {
            return DriveSyncDecision.UpToDate(remoteFile)
        }

        // First link on this device (or after disconnecting): pull cleanly if local DB is empty,
        // otherwise ask the user which copy to keep to prevent silent data loss.
        if (lastSyncedContentHash.isNullOrBlank()) {
            return if (localItemCount == 0) {
                DriveSyncDecision.PullRemote(remoteFile)
            } else {
                DriveSyncDecision.Conflict(remoteFile, localItemCount)
            }
        }

        val localChanged = localContentHash != lastSyncedContentHash
        val remoteChanged = if (!remoteHash.isNullOrBlank()) {
            remoteHash != lastSyncedContentHash
        } else {
            lastSyncedRemoteModifiedMillis == null ||
                remoteFile.modifiedTimeMillis != lastSyncedRemoteModifiedMillis
        }

        return when {
            !localChanged && !remoteChanged -> DriveSyncDecision.UpToDate(remoteFile)
            localChanged && !remoteChanged -> DriveSyncDecision.PushLocal(remoteFile.id)
            !localChanged && remoteChanged -> DriveSyncDecision.PullRemote(remoteFile)
            else -> DriveSyncDecision.Conflict(remoteFile, localItemCount)
        }
    }
}
