package com.zacksimpson.verses

import androidx.datastore.preferences.core.stringPreferencesKey

internal object VersePreferences {
    val CACHED_DATE = stringPreferencesKey("cached_date")
    val CACHED_REFERENCE = stringPreferencesKey("cached_reference")
    val CACHED_TEXT = stringPreferencesKey("cached_text")
}
