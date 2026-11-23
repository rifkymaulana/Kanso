package com.kanso.core.backup.drive

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed interface DriveAuthResult {
    data class Authorized(val accessToken: String) : DriveAuthResult
    data class NeedsUserConsent(val pendingIntent: PendingIntent) : DriveAuthResult
    data class Failed(val message: String, val isConfigurationError: Boolean = false) : DriveAuthResult
}

@Singleton
class GoogleDriveAuthHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun authorizationClient(activity: Activity?): AuthorizationClient {
        return if (activity != null) {
            Identity.getAuthorizationClient(activity)
        } else {
            Identity.getAuthorizationClient(context)
        }
    }

    suspend fun requestDriveAuthorization(activity: Activity? = null): DriveAuthResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
            .build()

        return suspendCancellableCoroutine { continuation ->
            authorizationClient(activity)
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    continuation.resume(mapAuthorizationResult(result))
                }
                .addOnFailureListener { error ->
                    if (!continuation.isActive) return@addOnFailureListener
                    continuation.resume(mapAuthException(error))
                }
        }
    }

    fun parseConsentIntent(data: Intent?, activity: Activity? = null): DriveAuthResult {
        if (data == null) {
            return DriveAuthResult.Failed(
                message = "Google Drive authorization returned no result. Verify Google Drive API is enabled, your email is in OAuth Test users, and your APK SHA-1 matches Google Cloud Console.",
                isConfigurationError = true
            )
        }
        return try {
            val result = authorizationClient(activity)
                .getAuthorizationResultFromIntent(data)
            mapAuthorizationResult(result)
        } catch (error: Exception) {
            mapAuthException(error)
        }
    }

    private fun mapAuthorizationResult(result: AuthorizationResult): DriveAuthResult {
        val pendingIntent = result.pendingIntent
        if (result.hasResolution() && pendingIntent != null) {
            return DriveAuthResult.NeedsUserConsent(pendingIntent)
        }
        val token = result.accessToken
        if (!token.isNullOrBlank()) {
            return DriveAuthResult.Authorized(token)
        }
        return DriveAuthResult.Failed(
            "Google Drive did not return an access token. Please try connecting again."
        )
    }

    private fun mapAuthException(error: Throwable): DriveAuthResult.Failed {
        if (error is ApiException) {
            return mapStatusCode(
                statusCode = error.statusCode,
                rawMessage = error.message
            )
        }
        return DriveAuthResult.Failed(
            message = error.message ?: "Failed to authorize Google Drive."
        )
    }

    companion object {
        const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val STATUS_SIGN_IN_FAILED = 12500
        const val STATUS_SIGN_IN_CANCELLED = 12501

        fun mapStatusCode(statusCode: Int, rawMessage: String? = null): DriveAuthResult.Failed {
            return when (statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR, STATUS_SIGN_IN_FAILED -> DriveAuthResult.Failed(
                    message = "Google OAuth configuration error (code $statusCode). Verify Android OAuth Client ID for com.kanso.app matches your APK SHA-1, Google Drive API is enabled, and your Google account is listed in Test users.",
                    isConfigurationError = true
                )

                CommonStatusCodes.NETWORK_ERROR -> DriveAuthResult.Failed(
                    message = "No internet connection. Check your connection and try again."
                )

                CommonStatusCodes.CANCELED, STATUS_SIGN_IN_CANCELLED -> DriveAuthResult.Failed(
                    message = "Google Drive authorization was cancelled (code $statusCode). If you did not tap Cancel, wait 5–15 minutes after saving Google Cloud OAuth changes and verify Test users + SHA-1.",
                    isConfigurationError = true
                )

                else -> DriveAuthResult.Failed(
                    message = "Google authorization failed (code $statusCode): ${
                        rawMessage?.takeIf { it.isNotBlank() } ?: CommonStatusCodes.getStatusCodeString(statusCode)
                    }",
                    isConfigurationError = false
                )
            }
        }
    }
}
