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
}
