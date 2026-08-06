package com.zacksimpson.verses

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class VerseBookmark(
    val id: String,
    val reference: String,
    val text: String,
    val translation: String,
    val createdAtMillis: Long,
)

/** Same fallback convention as VerseNote.resolvedTranslation(), for consistency. */
fun VerseBookmark.resolvedTranslation(): Translation = Translation.fromNameOrDefault(translation)

private val BOOKMARKS_KEY = stringPreferencesKey("verse_bookmarks")

/**
 * A bookmark is a toggle, not an appendable list like notes — a reference is either saved
 * or it isn't. Keyed by reference alone (no date field): unlike notes, which are
 * deliberately tied to a calendar day so a recurring day-of-year reference gets independent
 * entries, a bookmark's whole point is "save this passage," and there's only ever one.
 */
class VerseBookmarksRepository(private val dataStore: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(VerseBookmark.serializer())

    val bookmarks: Flow<List<VerseBookmark>> = dataStore.data.map { prefs -> prefs.readBookmarks() }

    private fun Preferences.readBookmarks(): List<VerseBookmark> =
        this[BOOKMARKS_KEY]?.let { raw -> runCatching { json.decodeFromString(serializer, raw) }.getOrNull() }
            ?: emptyList()

    /** Adds a bookmark for [reference] if none exists yet, otherwise removes it — the same
     *  method serves both the action sheet's toggle row and the detail screen's removal
     *  button, since toggling an existing bookmark is exactly what removal is. */
    suspend fun toggleBookmark(reference: String, text: String, translation: Translation) {
        dataStore.edit { prefs ->
            val current = prefs.readBookmarks()
            val updated = if (current.any { it.reference == reference }) {
                current.filterNot { it.reference == reference }
            } else {
                current + VerseBookmark(
                    id = UUID.randomUUID().toString(),
                    reference = reference,
                    text = text,
                    translation = translation.name,
                    createdAtMillis = System.currentTimeMillis(),
                )
            }
            prefs[BOOKMARKS_KEY] = json.encodeToString(serializer, updated)
        }
    }
}
