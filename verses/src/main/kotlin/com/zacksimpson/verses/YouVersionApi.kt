package com.zacksimpson.verses

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.Serializable

@Serializable
internal data class YouVersionPassageResponse(
    val id: String = "",
    val content: String = "",
    val reference: String = "",
)

/**
 * Client for the YouVersion Platform REST API (used for NIV/NASB), hit directly with
 * Ktor rather than their official Kotlin SDK — the SDK artifact isn't on the Light SDK
 * plugin's dependency allowlist, but Ktor already is, and this is a thin enough API that
 * a raw client is no more code than wrapping the SDK would have been.
 */
internal class YouVersionApi(private val appKey: String) {
    private val client = createBibleApiHttpClient()

    suspend fun fetchVerseText(versionId: Int, reference: String): Result<String> = runCatching {
        val passageId = UsfmReference.toPassageId(reference)
        val response = client.get(
            "https://api.youversion.com/v1/bibles/$versionId/passages/$passageId?format=text",
        ) {
            header("X-YVP-App-Key", appKey)
        }

        response.throwIfNotSuccess("YouVersion")

        val parsed: YouVersionPassageResponse = response.body()
        parsed.content.trim().ifEmpty {
            throw IllegalStateException("YouVersion API returned no passage text for '$reference'.")
        }
    }

    fun close() {
        client.close()
    }
}
