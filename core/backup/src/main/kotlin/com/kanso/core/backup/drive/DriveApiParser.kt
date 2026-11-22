package com.kanso.core.backup.drive

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

data class DriveRemoteFile(
    val id: String,
    val name: String,
    val modifiedTimeMillis: Long,
    val sizeBytes: Long,
    val contentHash: String?,
    val itemCount: Int?,
    val deviceModel: String?
)

data class DriveUserInfo(
    val emailAddress: String?,
    val displayName: String?
)

object DriveApiParser {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Serializable
    private data class DriveFileDto(
        val id: String,
        val name: String = "",
        val modifiedTime: String? = null,
        val size: String? = null,
        val appProperties: Map<String, String> = emptyMap()
    )

    @Serializable
    private data class DriveFileListDto(
        val files: List<DriveFileDto> = emptyList()
    )

    @Serializable
    private data class DriveAboutDto(
        val user: DriveAboutUserDto? = null
    )

    @Serializable
    private data class DriveAboutUserDto(
        val emailAddress: String? = null,
        val displayName: String? = null
    )

    @Serializable
    private data class DriveErrorEnvelopeDto(
        val error: DriveErrorDetailDto? = null
    )

    @Serializable
    private data class DriveErrorDetailDto(
        val code: Int? = null,
        val message: String? = null
    )

    @Serializable
    private data class DriveUploadMetadataDto(
        val name: String,
        val mimeType: String,
        val parents: List<String>? = null,
        val appProperties: Map<String, String>
    )

    fun parseFileListResponse(jsonText: String): List<DriveRemoteFile> {
        val dto = json.decodeFromString<DriveFileListDto>(jsonText)
        return dto.files.map { it.toDomain() }
    }

    fun parseFileMetadataResponse(jsonText: String): DriveRemoteFile {
        return json.decodeFromString<DriveFileDto>(jsonText).toDomain()
    }

    fun parseAboutUserResponse(jsonText: String): DriveUserInfo? {
        val dto = runCatching { json.decodeFromString<DriveAboutDto>(jsonText) }.getOrNull() ?: return null
        val user = dto.user ?: return null
        val email = user.emailAddress?.trim()?.takeIf { it.isNotEmpty() }
        val name = user.displayName?.trim()?.takeIf { it.isNotEmpty() }
        if (email == null && name == null) return null
        return DriveUserInfo(emailAddress = email, displayName = name)
    }

    fun parseApiErrorMessage(jsonText: String): String? {
        return runCatching {
            json.decodeFromString<DriveErrorEnvelopeDto>(jsonText).error?.message
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    fun buildMetadataJson(
        name: String,
        includeAppDataParent: Boolean,
        contentHash: String,
        itemCount: Int,
        schemaVersion: Int,
        deviceModel: String
    ): String {
        val metadata = DriveUploadMetadataDto(
            name = name,
            mimeType = "application/octet-stream",
            parents = if (includeAppDataParent) listOf("appDataFolder") else null,
            appProperties = buildMap {
                put("contentHash", contentHash)
                put("itemCount", itemCount.toString())
                put("schemaVersion", schemaVersion.toString())
                val cleanModel = deviceModel.trim().take(64)
                if (cleanModel.isNotEmpty()) {
                    put("deviceModel", cleanModel)
                }
            }
        )
        return json.encodeToString(metadata)
    }

    private fun DriveFileDto.toDomain(): DriveRemoteFile {
        val modifiedMillis = modifiedTime
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: 0L
        return DriveRemoteFile(
            id = id,
            name = name,
            modifiedTimeMillis = modifiedMillis,
            sizeBytes = size?.toLongOrNull() ?: 0L,
            contentHash = appProperties["contentHash"]?.takeIf { it.isNotBlank() },
            itemCount = appProperties["itemCount"]?.toIntOrNull(),
            deviceModel = appProperties["deviceModel"]?.takeIf { it.isNotBlank() }
        )
    }
}
