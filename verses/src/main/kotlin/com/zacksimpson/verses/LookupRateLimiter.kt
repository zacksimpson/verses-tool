package com.zacksimpson.verses

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate

internal const val DAILY_LOOKUP_LIMIT = 100

/** pure logic kept separate from the DataStore class below so it's unit testable
 *  without android. */
internal object LookupRateLimit {
    fun countForToday(storedDate: String?, storedCount: Int, today: String): Int =
        if (storedDate == today) storedCount else 0

    fun isAllowed(storedDate: String?, storedCount: Int, today: String, limit: Int = DAILY_LOOKUP_LIMIT): Boolean =
        countForToday(storedDate, storedCount, today) < limit

    fun nextCount(storedDate: String?, storedCount: Int, today: String): Int =
        countForToday(storedDate, storedCount, today) + 1

    fun dateKey(translation: Translation) = stringPreferencesKey("lookup_count_date_${translation.name}")
    fun countKey(translation: Translation) = intPreferencesKey("lookup_count_${translation.name}")

    /** today's call count for [translation] against an already-read [prefs] snapshot, for
     *  a screen that collects prefs reactively instead of reading DataStore itself. always
     *  0 for public domain sources. */
    fun countToday(prefs: Preferences, translation: Translation): Int {
        if (translation.source is TranslationSource.PublicDomain) return 0
        return countForToday(
            storedDate = prefs[dateKey(translation)],
            storedCount = prefs[countKey(translation)] ?: 0,
            today = LocalDate.now().toString(),
        )
    }
}

/**
 * a generous daily backstop on lookups against copyrighted translations, not a real
 * restriction, just a guard against a bug or stuck loop burning through the app's api
 * budget. public domain lookups are never limited.
 */
internal class LookupRateLimiter(private val dataStore: DataStore<Preferences>) {

    suspend fun shouldAllowLookup(translation: Translation): Boolean {
        if (translation.source is TranslationSource.PublicDomain) return true
        val prefs = dataStore.data.first()
        val today = LocalDate.now().toString()
        return LookupRateLimit.isAllowed(
            storedDate = prefs[LookupRateLimit.dateKey(translation)],
            storedCount = prefs[LookupRateLimit.countKey(translation)] ?: 0,
            today = today,
        )
    }

    suspend fun recordLookup(translation: Translation) {
        if (translation.source is TranslationSource.PublicDomain) return
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            val nextCount = LookupRateLimit.nextCount(
                storedDate = prefs[LookupRateLimit.dateKey(translation)],
                storedCount = prefs[LookupRateLimit.countKey(translation)] ?: 0,
                today = today,
            )
            prefs[LookupRateLimit.dateKey(translation)] = today
            prefs[LookupRateLimit.countKey(translation)] = nextCount
        }
    }
}
