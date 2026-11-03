package com.zacksimpson.verses

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object VersePreferences {
    // gates the one-time info screen shown the first time the search icon is tapped,
    // flipped true right before that navigation and never reset
    val HAS_SEEN_FALLBACK_TRANSLATION_INFO = booleanPreferencesKey("has_seen_fallback_translation_info")
    val CACHED_DATE = stringPreferencesKey("cached_date")
    val CACHED_REFERENCE = stringPreferencesKey("cached_reference")
    val CACHED_TEXT = stringPreferencesKey("cached_text")
    // which translation CACHED_TEXT is in, so a switch invalidates the cache even when
    // CACHED_DATE is still today
    val CACHED_TRANSLATION = stringPreferencesKey("cached_translation")
    val SELECTED_TRANSLATION = stringPreferencesKey("selected_translation")
    // separate from SELECTED_TRANSLATION (daily verse only), this is what the lookup
    // feature fetches in, defaulting to KJV
    val LOOKUP_TRANSLATION = stringPreferencesKey("lookup_translation")

    // one-slot cache for the last chapter fetched via the reference-tap shortcut, so
    // backing out and tapping the same reference again same day doesn't re-fetch. keyed
    // by translation+book+chapter together, not just date, since only one chapter fits
    // in the slot at a time
    val CACHED_CHAPTER_DATE = stringPreferencesKey("cached_chapter_date")
    val CACHED_CHAPTER_KEY = stringPreferencesKey("cached_chapter_key")
    val CACHED_CHAPTER_VERSES = stringPreferencesKey("cached_chapter_verses")
}

internal fun Preferences.selectedTranslation(): Translation =
    Translation.fromNameOrDefault(this[VersePreferences.SELECTED_TRANSLATION])

internal fun Preferences.cachedTranslation(): Translation =
    Translation.fromNameOrDefault(this[VersePreferences.CACHED_TRANSLATION])

/** how many verses are cached offline for [translation], 0 unless the cache's own
 *  translation matches. there's only one cache slot, the daily verse, overwritten once a
 *  day. falls back to 0 on a malformed reference instead of throwing, since this only
 *  feeds a read-only display. */
internal fun Preferences.cachedVerseCount(translation: Translation): Int {
    if (cachedTranslation() != translation) return 0
    val reference = this[VersePreferences.CACHED_REFERENCE] ?: return 0
    return runCatching {
        val range = UsfmReference.parseRange(UsfmReference.toPassageId(reference))
        range.endVerse - range.startVerse + 1
    }.getOrDefault(0)
}

internal fun Preferences.lookupTranslation(): Translation =
    this[VersePreferences.LOOKUP_TRANSLATION]
        ?.let { stored -> Translation.entries.firstOrNull { it.name == stored } }
        ?: Translation.KJV
