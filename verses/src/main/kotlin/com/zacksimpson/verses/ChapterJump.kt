package com.zacksimpson.verses

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

internal sealed class ChapterJumpResult {
    data class Success(val verses: List<VerseSegment>) : ChapterJumpResult()
    data class Failed(val message: String) : ChapterJumpResult()
}

private val chapterJson = Json { ignoreUnknownKeys = true }
private val chapterVersesSerializer = ListSerializer(VerseSegment.serializer())

private fun chapterCacheKey(translation: Translation, book: String, chapter: Int) =
    "${translation.name}|$book|$chapter"

/** book and chapter parsed off the front of a "Book Chapter" or "Book Chapter:Verse[-Verse]"
 *  reference, or null if it doesn't look like one. */
internal fun bookAndChapterFrom(reference: String): Pair<String, Int>? {
    val match = Regex("""^(.+?)\s+(\d+)""").find(reference) ?: return null
    val (book, chapter) = match.destructured
    return book to chapter.toInt()
}

/** resolves the whole chapter [reference] (a bare "Book Chapter", e.g. "2 Timothy 1")
 *  refers to, for [PassageScreen]'s own self-fetch mode when it's opened straight from a
 *  reference tap rather than a resolved lookup. reuses today's cached copy of the same
 *  chapter if there is one, so backing out and tapping the same reference again doesn't
 *  fetch or count against the rate limit twice. otherwise shares the same rate limiter as
 *  a manual lookup, so a fresh fetch on a copyrighted translation still counts against its
 *  daily limit. */
internal suspend fun resolveChapterForReference(
    dataStore: DataStore<Preferences>,
    fetcher: VerseFetcher,
    translation: Translation,
    reference: String,
): ChapterJumpResult {
    val (book, chapter) = bookAndChapterFrom(reference)
        ?: return ChapterJumpResult.Failed("Couldn't read that reference.")
    val cacheKey = chapterCacheKey(translation, book, chapter)
    val today = LocalDate.now().toString()

    val prefs = dataStore.data.first()
    if (prefs[VersePreferences.CACHED_CHAPTER_DATE] == today && prefs[VersePreferences.CACHED_CHAPTER_KEY] == cacheKey) {
        prefs[VersePreferences.CACHED_CHAPTER_VERSES]
            ?.let { raw -> runCatching { chapterJson.decodeFromString(chapterVersesSerializer, raw) }.getOrNull() }
            ?.let { return ChapterJumpResult.Success(it) }
    }

    if (!fetcher.isConfigured(translation)) {
        return ChapterJumpResult.Failed(fetcher.missingKeyMessage(translation))
    }

    // reuses the prefs already read above instead of a second DataStore read, same as
    // shouldAllowLookup would do internally
    val isAllowed = translation.source is TranslationSource.PublicDomain || LookupRateLimit.isAllowed(
        storedDate = prefs[LookupRateLimit.dateKey(translation)],
        storedCount = prefs[LookupRateLimit.countKey(translation)] ?: 0,
        today = today,
    )
    if (!isAllowed) {
        return ChapterJumpResult.Failed(dailyLimitReachedMessage(translation))
    }

    return fetcher.fetchWholeChapter(translation, book, chapter).fold(
        onSuccess = { verses ->
            LookupRateLimiter(dataStore).recordLookup(translation)
            dataStore.edit { p ->
                p[VersePreferences.CACHED_CHAPTER_DATE] = today
                p[VersePreferences.CACHED_CHAPTER_KEY] = cacheKey
                p[VersePreferences.CACHED_CHAPTER_VERSES] = chapterJson.encodeToString(chapterVersesSerializer, verses)
            }
            ChapterJumpResult.Success(verses)
        },
        onFailure = { ChapterJumpResult.Failed("Couldn't load $reference. Try again shortly.") },
    )
}
