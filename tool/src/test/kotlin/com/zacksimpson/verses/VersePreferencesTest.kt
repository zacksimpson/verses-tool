package com.zacksimpson.verses

import androidx.datastore.preferences.core.preferencesOf
import kotlin.test.Test
import kotlin.test.assertEquals

class VersePreferencesTest {

    @Test
    fun `lookupTranslation defaults to KJV when nothing is stored`() {
        assertEquals(Translation.KJV, preferencesOf().lookupTranslation())
    }

    @Test
    fun `lookupTranslation accepts a stored public domain translation`() {
        val prefs = preferencesOf(VersePreferences.LOOKUP_TRANSLATION to Translation.BSB.name)
        assertEquals(Translation.BSB, prefs.lookupTranslation())
    }

    @Test
    fun `lookupTranslation accepts a stored copyrighted translation`() {
        val prefs = preferencesOf(VersePreferences.LOOKUP_TRANSLATION to Translation.ESV.name)
        assertEquals(Translation.ESV, prefs.lookupTranslation())
    }

    @Test
    fun `cachedVerseCount is zero when nothing is cached`() {
        assertEquals(0, preferencesOf().cachedVerseCount(Translation.ESV))
    }

    @Test
    fun `cachedVerseCount is zero when the cache belongs to a different translation`() {
        val prefs = preferencesOf(
            VersePreferences.CACHED_TRANSLATION to Translation.NIV.name,
            VersePreferences.CACHED_REFERENCE to "John 3:16",
        )
        assertEquals(0, prefs.cachedVerseCount(Translation.ESV))
    }

    @Test
    fun `cachedVerseCount is 1 for a single-verse reference`() {
        val prefs = preferencesOf(
            VersePreferences.CACHED_TRANSLATION to Translation.ESV.name,
            VersePreferences.CACHED_REFERENCE to "John 3:16",
        )
        assertEquals(1, prefs.cachedVerseCount(Translation.ESV))
    }

    @Test
    fun `cachedVerseCount counts every verse in a multi-verse range`() {
        val prefs = preferencesOf(
            VersePreferences.CACHED_TRANSLATION to Translation.ESV.name,
            VersePreferences.CACHED_REFERENCE to "John 3:16-18",
        )
        assertEquals(3, prefs.cachedVerseCount(Translation.ESV))
    }

    @Test
    fun `cachedVerseCount falls back to 0 on a malformed reference instead of throwing`() {
        val prefs = preferencesOf(
            VersePreferences.CACHED_TRANSLATION to Translation.ESV.name,
            VersePreferences.CACHED_REFERENCE to "not a reference",
        )
        assertEquals(0, prefs.cachedVerseCount(Translation.ESV))
    }
}
