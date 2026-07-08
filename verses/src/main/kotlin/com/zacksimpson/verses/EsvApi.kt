package com.zacksimpson.verses

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

@Serializable
internal data class EsvPassageResponse(
    val query: String = "",
    val canonical: String = "",
    val passages: List<String> = emptyList(),
)

internal class EsvApi(private val apiKey: String) {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun fetchVerseText(reference: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(reference, UTF_8.name())
        val response = client.get(
            "https://api.esv.org/v3/passage/text/" +
                "?q=$encoded" +
                "&include-headings=false" +
                "&include-footnotes=false" +
                "&include-verse-numbers=false" +
                "&include-passage-references=false" +
                "&include-short-copyright=true",
        ) {
            header("Authorization", "Token $apiKey")
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("ESV API HTTP ${response.status.value}: $body")
        }

        val parsed: EsvPassageResponse = response.body()
        parsed.passages.firstOrNull()?.trim()
            ?: throw IllegalStateException("ESV API returned no passage text for '$reference'.")
    }

    fun close() {
        client.close()
    }
}
