package com.kanso.core.backup

import com.google.android.gms.common.api.CommonStatusCodes
import com.kanso.core.backup.drive.GoogleDriveAuthHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveAuthErrorMapperTest {

    @Test
    fun `developer error and sign in failed codes flag configuration guidance`() {
        val devErr = GoogleDriveAuthHelper.mapStatusCode(CommonStatusCodes.DEVELOPER_ERROR)
        assertTrue(devErr.isConfigurationError)
        assertTrue(devErr.message.contains("10"))
        assertTrue(devErr.message.contains("SHA-1"))

        val signInFailed = GoogleDriveAuthHelper.mapStatusCode(GoogleDriveAuthHelper.STATUS_SIGN_IN_FAILED)
        assertTrue(signInFailed.isConfigurationError)
        assertTrue(signInFailed.message.contains("12500"))
    }

    @Test
    fun `canceled status codes mention configuration wait time cleanly`() {
        val canceled = GoogleDriveAuthHelper.mapStatusCode(CommonStatusCodes.CANCELED)
        assertTrue(canceled.isConfigurationError)
        assertTrue(canceled.message.contains("16"))

        val signInCanceled = GoogleDriveAuthHelper.mapStatusCode(GoogleDriveAuthHelper.STATUS_SIGN_IN_CANCELLED)
        assertTrue(signInCanceled.isConfigurationError)
        assertTrue(signInCanceled.message.contains("12501"))
    }

    @Test
    fun `network error does not flag configuration error`() {
        val netErr = GoogleDriveAuthHelper.mapStatusCode(CommonStatusCodes.NETWORK_ERROR)
        assertFalse(netErr.isConfigurationError)
        assertTrue(netErr.message.contains("internet", ignoreCase = true))
    }

    @Test
    fun `unknown status code includes numeric code and fallback string`() {
        val unknown = GoogleDriveAuthHelper.mapStatusCode(999, null)
        assertFalse(unknown.isConfigurationError)
        assertTrue(unknown.message.contains("999"))
    }
}
