package com.kanso.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubReleaseParserTest {

    @Test
    fun `parses latest GitHub release json picking release apk and sha256 asset`() {
        val sampleJson = """
            {
              "tag_name": "v1.1.0",
              "name": "Kanso v1.1.0",
              "body": "Added in-app auto-update support.",
              "html_url": "https://github.com/rifkymaulana/Kanso/releases/tag/v1.1.0",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "id": 1001,
                  "name": "Kanso-v1.1.0-release.apk.sha256",
                  "size": 96,
                  "url": "https://api.github.com/repos/rifkymaulana/Kanso/releases/assets/1001",
                  "browser_download_url": "https://github.com/rifkymaulana/Kanso/releases/download/v1.1.0/Kanso-v1.1.0-release.apk.sha256"
                },
                {
                  "id": 1002,
                  "name": "Kanso-v1.1.0-release.apk",
                  "size": 15800000,
                  "url": "https://api.github.com/repos/rifkymaulana/Kanso/releases/assets/1002",
                  "browser_download_url": "https://github.com/rifkymaulana/Kanso/releases/download/v1.1.0/Kanso-v1.1.0-release.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = GitHubReleaseParser.parseLatestRelease(sampleJson)
        assertNotNull(parsed)
        assertEquals("v1.1.0", parsed?.tagName)
        assertEquals("1.1.0", parsed?.versionName)
        assertEquals("Kanso-v1.1.0-release.apk", parsed?.apkAssetName)
        assertEquals(15_800_000L, parsed?.apkSizeBytes)
        assertEquals("https://api.github.com/repos/rifkymaulana/Kanso/releases/assets/1002", parsed?.apkApiUrl)
        assertEquals("https://api.github.com/repos/rifkymaulana/Kanso/releases/assets/1001", parsed?.sha256ApiUrl)
    }

    @Test
    fun `ignores draft releases or releases without apk assets`() {
        val draftJson = """
            {"tag_name":"v1.2.0","draft":true,"assets":[{"name":"Kanso-v1.2.0-release.apk","url":"https://api.github.com/a"}]}
        """.trimIndent()
        assertNull(GitHubReleaseParser.parseLatestRelease(draftJson))

        val noApkJson = """
            {"tag_name":"v1.2.0","draft":false,"assets":[{"name":"notes.txt","url":"https://api.github.com/a"}]}
        """.trimIndent()
        assertNull(GitHubReleaseParser.parseLatestRelease(noApkJson))
    }

    @Test
    fun `parses sha256sum output and rejects malformed digests`() {
        val valid = "DE28938C8396547C08D2F8AECACF5955AEE03CEA60FD844788B93D7988FDE871  dist/Kanso-v1.0.0-release.apk\n"
        assertEquals(
            "de28938c8396547c08d2f8aecacf5955aee03cea60fd844788b93d7988fde871",
            GitHubReleaseParser.parseExpectedSha256(valid)
        )
        assertNull(GitHubReleaseParser.parseExpectedSha256("not-a-sha256"))
        assertNull(GitHubReleaseParser.parseExpectedSha256(""))
    }
}
