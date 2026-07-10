package com.zacksimpson.verses

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

@Serializable
internal data class BibleApiComVerse(
    val book_id: String = "",
    val book_name: String = "",
    val chapter: Int = 0,
    val verse: Int = 0,
    val text: String = "",
)

@Serializable
internal data class BibleApiComResponse(
    val reference: String = "",
    val verses: List<BibleApiComVerse> = emptyList(),
    val text: String = "",
)

/** A whole chapter's verses, in order, as (verse number, text) pairs. */
internal data class BibleChapter(
    val book: String,
    val chapter: Int,
    val verses: List<Pair<Int, String>>,
)

/**
 * Client for bible-api.com (the public-domain KJV translation) — free, keyless, and per
 * its own response payload ("translation_note": "Public Domain") carries no rate limit or
 * storage restriction, unlike the Esv/YouVersion clients. That's what makes it the only
 * one of the three safe to also expose a whole-chapter fetch for (see
 * VerseFetcher.fetchChapter) — there's no shared quota or license scope to protect here.
 */
internal class BibleApiComApi {
    private val client = createBibleApiHttpClient()

    suspend fun fetchVerseText(reference: String): Result<String> = runCatching {
        val response = client.get(passageUrl(reference))
        response.throwIfNotSuccess("bible-api.com")
        val parsed: BibleApiComResponse = response.body()
        parsed.text.trim().ifEmpty {
            throw IllegalStateException("bible-api.com returned no passage text for '$reference'.")
        }
    }

    suspend fun fetchChapter(book: String, chapter: Int): Result<BibleChapter> = runCatching {
        val reference = "$book $chapter"
        val response = client.get(passageUrl(reference))
        response.throwIfNotSuccess("bible-api.com")
        val parsed: BibleApiComResponse = response.body()
        if (parsed.verses.isEmpty()) {
            throw IllegalStateException("bible-api.com returned no verses for '$reference'.")
        }
        BibleChapter(
            book = book,
            chapter = chapter,
            verses = parsed.verses.map { it.verse to it.text.trim() },
        )
    }

    // URLEncoder is form (query-string) encoding, where space becomes "+" — valid in a
    // query string but not a path segment, so it's converted to "%20" here. Confirmed
    // live against bible-api.com that fully percent-encoded references (spaces, colons,
    // commas all escaped) parse identically to the lightly-escaped examples in their docs.
    private fun passageUrl(reference: String): String {
        val encoded = URLEncoder.encode(reference, UTF_8.name()).replace("+", "%20")
        return "https://bible-api.com/$encoded?translation=kjv"
    }

    fun close() {
        client.close()
    }
}
