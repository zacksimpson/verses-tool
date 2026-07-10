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
    // this is which translation the (not-yet-built) lookup feature fetches in, defaulting
    // to KJV rather than ESV since lookup is meant to default to public domain.
    val LOOKUP_TRANSLATION = stringPreferencesKey("lookup_translation")
}

internal fun Preferences.selectedTranslation(): Translation =
    Translation.fromNameOrDefault(this[VersePreferences.SELECTED_TRANSLATION])

internal fun Preferences.cachedTranslation(): Translation =
    Translation.fromNameOrDefault(this[VersePreferences.CACHED_TRANSLATION])

internal fun Preferences.lookupTranslation(): Translation =
    this[VersePreferences.LOOKUP_TRANSLATION]
        ?.let { stored -> Translation.entries.firstOrNull { it.name == stored } }
        ?: Translation.KJV
