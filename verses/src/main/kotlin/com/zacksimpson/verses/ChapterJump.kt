package com.zacksimpson.verses

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal sealed class ChapterJumpResult {
    data class Success(val verses: List<VerseSegment>) : ChapterJumpResult()
    data class Failed(val message: String) : ChapterJumpResult()
}

/** book and chapter parsed off the front of a "Book Chapter" or "Book Chapter:Verse[-Verse]"
 *  reference, or null if it doesn't look like one. */
internal fun bookAndChapterFrom(reference: String): Pair<String, Int>? {
    val match = Regex("""^(.+?)\s+(\d+)""").find(reference) ?: return null
    val (book, chapter) = match.destructured
    return book to chapter.toInt()
}

/** resolves the whole chapter [reference] (a bare "Book Chapter", e.g. "2 Timothy 1")
 *  refers to, for [PassageScreen]'s own self-fetch mode when it's opened straight from a
 *  reference tap rather than a resolved lookup. shares the same rate limiter as a manual
 *  lookup, so tapping a copyrighted translation's reference still counts against its
 *  daily limit. */
internal suspend fun resolveChapterForReference(
    dataStore: DataStore<Preferences>,
    fetcher: VerseFetcher,
    translation: Translation,
    reference: String,
): ChapterJumpResult {
    val (book, chapter) = bookAndChapterFrom(reference)
        ?: return ChapterJumpResult.Failed("Couldn't read that reference.")

    val rateLimiter = LookupRateLimiter(dataStore)
    if (!rateLimiter.shouldAllowLookup(translation)) {
        return ChapterJumpResult.Failed(dailyLimitReachedMessage(translation))
    }

    return fetcher.fetchWholeChapter(translation, book, chapter).fold(
        onSuccess = { verses ->
            rateLimiter.recordLookup(translation)
            ChapterJumpResult.Success(verses)
        },
        onFailure = { ChapterJumpResult.Failed("Couldn't load $reference. Try again shortly.") },
    )
}
