package com.kanso.core.backup.drive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

class DriveUnauthorizedException(message: String) : IOException(message)

class DriveNotFoundException(message: String) : IOException(message)

class DriveHttpException(
    val statusCode: Int,
    message: String
) : IOException(message)

@Singleton
class GoogleDriveRestClient @Inject constructor() {

    suspend fun findSyncFile(accessToken: String): DriveRemoteFile? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name='$SYNC_FILE_NAME' and trashed=false", "UTF-8")
        val fields = URLEncoder.encode("files(id,name,modifiedTime,size,appProperties)", "UTF-8")
        val orderBy = URLEncoder.encode("modifiedTime desc", "UTF-8")
        val url = "$BASE_DRIVE_URL/files?spaces=appDataFolder&q=$q&fields=$fields&orderBy=$orderBy&pageSize=5"

        val body = executeGetText(url, accessToken)
        DriveApiParser.parseFileListResponse(body).firstOrNull()
    }

    suspend fun fetchUserInfo(accessToken: String): DriveUserInfo? = withContext(Dispatchers.IO) {
        val fields = URLEncoder.encode("user(emailAddress,displayName)", "UTF-8")
        val url = "$BASE_DRIVE_URL/about?fields=$fields"
        runCatching {
            val body = executeGetText(url, accessToken)
            DriveApiParser.parseAboutUserResponse(body)
        }.getOrNull()
    }

    suspend fun createSyncFile(
        accessToken: String,
        payload: ByteArray,
        contentHash: String,
        itemCount: Int,
        schemaVersion: Int,
        deviceModel: String
    ): DriveRemoteFile = withContext(Dispatchers.IO) {
        val fields = URLEncoder.encode("id,name,modifiedTime,size,appProperties", "UTF-8")
        val url = "$BASE_UPLOAD_URL/files?uploadType=multipart&fields=$fields"
        val metadataJson = DriveApiParser.buildMetadataJson(
            name = SYNC_FILE_NAME,
            includeAppDataParent = true,
            contentHash = contentHash,
            itemCount = itemCount,
            schemaVersion = schemaVersion,
            deviceModel = deviceModel
        )
        executeMultipartUpload(
            urlString = url,
            accessToken = accessToken,
            usePatchOverride = false,
            metadataJson = metadataJson,
            payload = payload
        )
    }

    suspend fun updateSyncFile(
        accessToken: String,
        fileId: String,
        payload: ByteArray,
        contentHash: String,
        itemCount: Int,
        schemaVersion: Int,
        deviceModel: String
    ): DriveRemoteFile = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(fileId, "UTF-8")
        val fields = URLEncoder.encode("id,name,modifiedTime,size,appProperties", "UTF-8")
        val url = "$BASE_UPLOAD_URL/files/$encodedId?uploadType=multipart&fields=$fields"
        val metadataJson = DriveApiParser.buildMetadataJson(
            name = SYNC_FILE_NAME,
            includeAppDataParent = false,
            contentHash = contentHash,
            itemCount = itemCount,
            schemaVersion = schemaVersion,
            deviceModel = deviceModel
        )
        executeMultipartUpload(
            urlString = url,
            accessToken = accessToken,
            usePatchOverride = true,
            metadataJson = metadataJson,
            payload = payload
        )
    }

    suspend fun downloadFile(accessToken: String, fileId: String): ByteArray = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(fileId, "UTF-8")
        val url = "$BASE_DRIVE_URL/files/$encodedId?alt=media"
        executeGetBytes(url, accessToken)
    }

    private fun executeGetText(urlString: String, accessToken: String): String {
        return executeGetBytes(urlString, accessToken).decodeToString()
    }

    private fun executeGetBytes(urlString: String, accessToken: String): ByteArray {
        val connection = openConnection(urlString, accessToken)
        return try {
            connection.requestMethod = "GET"
            val status = connection.responseCode
            if (status in 200..299) {
                connection.inputStream.use { it.readBytes() }
            } else {
                throw mapHttpFailure(status, connection)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun executeMultipartUpload(
        urlString: String,
        accessToken: String,
        usePatchOverride: Boolean,
        metadataJson: String,
        payload: ByteArray
    ): DriveRemoteFile {
        val boundary = "kanso_drive_boundary_${System.currentTimeMillis()}"
        val bodyBytes = buildMultipartBody(boundary, metadataJson, payload)
        val connection = openConnection(urlString, accessToken)
        return try {
            connection.requestMethod = "POST"
            if (usePatchOverride) {
                // HttpURLConnection rejects PATCH on older Android stacks; Drive v3 honors method override.
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            }
            connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            connection.doOutput = true
            connection.doInput = true
            connection.useCaches = false
            connection.setFixedLengthStreamingMode(bodyBytes.size)
            connection.outputStream.use { out ->
                out.write(bodyBytes)
                out.flush()
            }

            val status = connection.responseCode
            if (status in 200..299) {
                val responseText = connection.inputStream.use { it.readBytes().decodeToString() }
                DriveApiParser.parseFileMetadataResponse(responseText)
            } else {
                throw mapHttpFailure(status, connection)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildMultipartBody(
        boundary: String,
        metadataJson: String,
        payload: ByteArray
    ): ByteArray {
        val out = ByteArrayOutputStream(metadataJson.length + payload.size + 512)
        val crlf = "\r\n"
        out.write("--$boundary$crlf".toByteArray(Charsets.UTF_8))
        out.write("Content-Type: application/json; charset=UTF-8$crlf$crlf".toByteArray(Charsets.UTF_8))
        out.write(metadataJson.toByteArray(Charsets.UTF_8))
        out.write(crlf.toByteArray(Charsets.UTF_8))

        out.write("--$boundary$crlf".toByteArray(Charsets.UTF_8))
        out.write("Content-Type: application/octet-stream$crlf$crlf".toByteArray(Charsets.UTF_8))
        out.write(payload)
        out.write("$crlf--$boundary--$crlf".toByteArray(Charsets.UTF_8))
        return out.toByteArray()
    }

    private fun openConnection(urlString: String, accessToken: String): HttpURLConnection {
        return (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun mapHttpFailure(statusCode: Int, connection: HttpURLConnection): IOException {
        val rawError = runCatching {
            connection.errorStream?.use { it.readBytes().decodeToString() }
        }.getOrNull().orEmpty()
        val apiMessage = DriveApiParser.parseApiErrorMessage(rawError)

        return when (statusCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                DriveUnauthorizedException(
                    apiMessage ?: "Google Drive session expired. Please reconnect or tap Sync Now."
                )
            }

            HttpURLConnection.HTTP_NOT_FOUND -> {
                DriveNotFoundException(apiMessage ?: "Sync file not found in Google Drive.")
            }

            else -> {
                DriveHttpException(
                    statusCode = statusCode,
                    message = apiMessage ?: "Google Drive request failed (HTTP $statusCode)."
                )
            }
        }
    }

    companion object {
        const val SYNC_FILE_NAME = "kanso-cloud-sync.kanso.enc"
        private const val BASE_DRIVE_URL = "https://www.googleapis.com/drive/v3"
        private const val BASE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3"
        private const val TIMEOUT_MS = 20_000
    }
}
