package com.zacksimpson.verses

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.Serializable
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

@Serializable
internal data class EsvPassageResponse(
    val query: String = "",
    val canonical: String = "",
    val passages: List<String> = emptyList(),
)

internal class EsvApi(private val apiKey: String) {
    private val client = createBibleApiHttpClient()

    suspend fun fetchVerseText(reference: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(reference, UTF_8.name())
        val response = client.get(
            "https://api.esv.org/v3/passage/text/" +
                "?q=$encoded" +
                "&include-headings=false" +
                "&include-footnotes=false" +
                "&include-verse-numbers=false" +
                "&include-passage-references=false" +
                // We attach "(ESV)" to the reference line ourselves instead — see VersesHomeScreen.
                "&include-short-copyright=false",
        ) {
            header("Authorization", "Token $apiKey")
        }

        response.throwIfNotSuccess("ESV")

        val parsed: EsvPassageResponse = response.body()
        parsed.passages.firstOrNull()?.trim()
            ?: throw IllegalStateException("ESV API returned no passage text for '$reference'.")
    }

    fun close() {
        client.close()
    }
}
