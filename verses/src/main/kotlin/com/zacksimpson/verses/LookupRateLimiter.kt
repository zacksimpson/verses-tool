package com.zacksimpson.verses

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate

internal const val DAILY_LOOKUP_LIMIT = 100

/** Pure logic, kept separate from the DataStore-touching class below so it's unit
 *  testable without Android — whether a stored count still applies today, and what the
 *  next count is after another lookup. */
internal object LookupRateLimit {
    fun countForToday(storedDate: String?, storedCount: Int, today: String): Int =
        if (storedDate == today) storedCount else 0

    fun isAllowed(storedDate: String?, storedCount: Int, today: String, limit: Int = DAILY_LOOKUP_LIMIT): Boolean =
        countForToday(storedDate, storedCount, today) < limit

    fun nextCount(storedDate: String?, storedCount: Int, today: String): Int =
        countForToday(storedDate, storedCount, today) + 1
}

/**
 * A generous, invisible daily backstop on lookups against copyrighted (Esv/YouVersion)
 * translations — not a day-to-day restriction (100/day is far beyond what a real person
 * would hit at this app's scale), just a guard against a bug or stuck loop burning
 * through the app's shared API budget. Public domain lookups are never limited, so this
 * is never even consulted for TranslationSource.PublicDomain — same DataStore-backed
 * "reset on date change" shape as the existing verse-of-the-day cache.
 */
internal class LookupRateLimiter(private val dataStore: DataStore<Preferences>) {

    suspend fun shouldAllowLookup(translation: Translation): Boolean {
        if (translation.source is TranslationSource.PublicDomain) return true
        val prefs = dataStore.data.first()
        val today = LocalDate.now().toString()
        return LookupRateLimit.isAllowed(
            storedDate = prefs[dateKey(translation)],
            storedCount = prefs[countKey(translation)] ?: 0,
            today = today,
        )
    }

    suspend fun recordLookup(translation: Translation) {
        if (translation.source is TranslationSource.PublicDomain) return
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            val nextCount = LookupRateLimit.nextCount(
                storedDate = prefs[dateKey(translation)],
                storedCount = prefs[countKey(translation)] ?: 0,
                today = today,
            )
            prefs[dateKey(translation)] = today
            prefs[countKey(translation)] = nextCount
        }
    }

    private fun dateKey(translation: Translation) = stringPreferencesKey("lookup_count_date_${translation.name}")
    private fun countKey(translation: Translation) = intPreferencesKey("lookup_count_${translation.name}")
}
