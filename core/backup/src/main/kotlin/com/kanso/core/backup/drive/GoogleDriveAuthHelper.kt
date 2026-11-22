package com.kanso.core.backup.drive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
    suspend fun requestDriveAuthorization(): DriveAuthResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
            .build()

        return suspendCancellableCoroutine { continuation ->
            Identity.getAuthorizationClient(context)
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

    fun parseConsentIntent(data: Intent?): DriveAuthResult {
        if (data == null) {
            return DriveAuthResult.Failed("Google authorization returned empty result.")
        }
        return try {
            val result = Identity.getAuthorizationClient(context)
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
            return when (error.statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR, 12500 -> DriveAuthResult.Failed(
                    message = "Google OAuth client is not configured for this app signature yet (code ${error.statusCode}). Create an Android OAuth 2.0 Client ID in Google Cloud Console for com.kanso.app with your APK SHA-1 and enable Google Drive API.",
                    isConfigurationError = true
                )

                CommonStatusCodes.NETWORK_ERROR -> DriveAuthResult.Failed(
                    message = "No internet connection. Check your connection and try again."
                )

                CommonStatusCodes.CANCELED -> DriveAuthResult.Failed(
                    message = "Google Drive sign-in was cancelled."
                )

                else -> DriveAuthResult.Failed(
                    message = "Google authorization failed (code ${error.statusCode}): ${CommonStatusCodes.getStatusCodeString(error.statusCode)}",
                    isConfigurationError = false
                )
            }
        }
        return DriveAuthResult.Failed(
            message = error.message ?: "Failed to authorize Google Drive."
        )
    }

    companion object {
        const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
