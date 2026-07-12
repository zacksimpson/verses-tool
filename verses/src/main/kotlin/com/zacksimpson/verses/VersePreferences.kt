package com.zacksimpson.verses

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

internal object VersePreferences {
    val CACHED_DATE = stringPreferencesKey("cached_date")
    val CACHED_REFERENCE = stringPreferencesKey("cached_reference")
    val CACHED_TEXT = stringPreferencesKey("cached_text")
    // Which translation CACHED_TEXT is in — a translation switch must invalidate the
    // cache even if CACHED_DATE is still today, or the old translation's text would
    // flash under the new translation's label.
    val CACHED_TRANSLATION = stringPreferencesKey("cached_translation")
    val SELECTED_TRANSLATION = stringPreferencesKey("selected_translation")
    // Separate from SELECTED_TRANSLATION, which is exclusively about the daily verse —
    // this is which translation the verse lookup feature fetches in, defaulting to KJV
    // (any translation is a valid choice here, same list as SELECTED_TRANSLATION's picker).
    val LOOKUP_TRANSLATION = stringPreferencesKey("lookup_translation")
}

internal fun Preferences.selectedTranslation(): Translation =
    Translation.fromNameOrDefault(this[VersePreferences.SELECTED_TRANSLATION])

internal fun Preferences.cachedTranslation(): Translation =
    Translation.fromNameOrDefault(this[VersePreferences.CACHED_TRANSLATION])

/** How many verses are currently cached offline for [translation] — 0 unless the cache's own
 *  translation matches (there's only one durable cache slot, the daily verse, shared across
 *  every translation and overwritten once a day; the lookup flow never persists what it
 *  fetches). Parses [VersePreferences.CACHED_REFERENCE]'s verse range via [UsfmReference] —
 *  falls back to 0 on a malformed/legacy reference rather than throwing, since this only
 *  feeds a read-only display (Settings → Advanced → View API Logs). */
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
