package com.kanso.core.backup

import com.kanso.core.backup.drive.DriveApiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DriveApiParserTest {

    @Test
    fun `parseFileListResponse extracts metadata and appProperties cleanly`() {
        val rawJson = """
            {
              "files": [
                {
                  "id": "file-123",
                  "name": "kanso-cloud-sync.kanso.enc",
                  "modifiedTime": "2026-11-22T17:30:00Z",
                  "size": "48192",
                  "appProperties": {
                    "contentHash": "abc123def",
                    "itemCount": "142",
                    "deviceModel": "Pixel 9"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = DriveApiParser.parseFileListResponse(rawJson)
        assertEquals(1, parsed.size)
        val file = parsed.first()
        assertEquals("file-123", file.id)
        assertEquals("kanso-cloud-sync.kanso.enc", file.name)
        assertEquals(Instant.parse("2026-11-22T17:30:00Z").toEpochMilli(), file.modifiedTimeMillis)
        assertEquals(48192L, file.sizeBytes)
        assertEquals("abc123def", file.contentHash)
        assertEquals(142, file.itemCount)
        assertEquals("Pixel 9", file.deviceModel)
    }

    @Test
    fun `parseFileMetadataResponse handles empty file list and missing optional fields`() {
        val emptyList = DriveApiParser.parseFileListResponse("""{ "files": [] }""")
        assertTrue(emptyList.isEmpty())

        val minimal = DriveApiParser.parseFileMetadataResponse(
            """{ "id": "id-1", "name": "kanso-cloud-sync.kanso.enc" }"""
        )
        assertEquals("id-1", minimal.id)
        assertEquals(0L, minimal.modifiedTimeMillis)
        assertEquals(0L, minimal.sizeBytes)
        assertNull(minimal.contentHash)
        assertNull(minimal.itemCount)
        assertNull(minimal.deviceModel)
    }

    @Test
    fun `parseAboutUserResponse extracts user info or returns null when absent`() {
        val valid = DriveApiParser.parseAboutUserResponse(
            """{ "user": { "emailAddress": "rifky@example.com", "displayName": "Rifky" } }"""
        )
        assertNotNull(valid)
        assertEquals("rifky@example.com", valid?.emailAddress)
        assertEquals("Rifky", valid?.displayName)

        assertNull(DriveApiParser.parseAboutUserResponse("""{}"""))
        assertNull(DriveApiParser.parseAboutUserResponse("""{ "user": { "emailAddress": "  " } }"""))
    }

    @Test
    fun `parseApiErrorMessage extracts Drive API error message`() {
        val message = DriveApiParser.parseApiErrorMessage(
            """
            {
              "error": {
                "code": 403,
                "message": "Google Drive API has not been used in project 123 before."
              }
            }
            """.trimIndent()
        )
        assertEquals("Google Drive API has not been used in project 123 before.", message)
        assertNull(DriveApiParser.parseApiErrorMessage("not json"))
    }

    @Test
    fun `buildMetadataJson includes appDataFolder parent only on creation`() {
        val createJson = DriveApiParser.buildMetadataJson(
            name = "kanso-cloud-sync.kanso.enc",
            includeAppDataParent = true,
            contentHash = "deadbeef",
            itemCount = 25,
            schemaVersion = 2,
            deviceModel = "Galaxy S24"
        )
        assertTrue(createJson.contains("\"parents\":[\"appDataFolder\"]"))
        assertTrue(createJson.contains("\"contentHash\":\"deadbeef\""))
        assertTrue(createJson.contains("\"itemCount\":\"25\""))

        val updateJson = DriveApiParser.buildMetadataJson(
            name = "kanso-cloud-sync.kanso.enc",
            includeAppDataParent = false,
            contentHash = "deadbeef",
            itemCount = 25,
            schemaVersion = 2,
            deviceModel = "Galaxy S24"
        )
        assertFalse(updateJson.contains("\"parents\""))
    }
}
