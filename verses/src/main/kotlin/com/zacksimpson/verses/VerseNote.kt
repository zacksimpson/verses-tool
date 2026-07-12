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
data class VerseNote(
    val id: String,
    val date: String,
    val reference: String,
    val text: String,
    val createdAtMillis: Long,
    // Nullable/additive so notes stored before this field existed keep deserializing —
    // resolvedTranslation() below is how callers should read it, not this directly.
    val translation: String? = null,
)

/** Notes predating this field don't know their translation — same fallback as
 *  Translation.fromNameOrDefault, applied here since VerseNote stores the raw name. */
fun VerseNote.resolvedTranslation(): Translation = Translation.fromNameOrDefault(translation)

private val NOTES_KEY = stringPreferencesKey("verse_notes")

/**
 * Free-text notes on a verse-of-the-day date, keyed by date rather than reference — if
 * the day-of-year cycle ever repeats a reference in a future year, that occurrence gets
 * its own independent notes rather than reusing old ones. A single date can have
 * multiple notes: "Add Notes" always appends rather than overwriting.
 */
class VerseNotesRepository(private val dataStore: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(VerseNote.serializer())

    val notes: Flow<List<VerseNote>> = dataStore.data.map { prefs -> prefs.readNotes() }

    private fun Preferences.readNotes(): List<VerseNote> =
        this[NOTES_KEY]?.let { raw -> runCatching { json.decodeFromString(serializer, raw) }.getOrNull() }
            ?: emptyList()

    /** Always appends a new note; blank text is a no-op. */
    suspend fun addNote(date: String, reference: String, text: String, translation: Translation) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            val updated = prefs.readNotes() + VerseNote(
                id = UUID.randomUUID().toString(),
                date = date,
                reference = reference,
                text = trimmed,
                createdAtMillis = System.currentTimeMillis(),
                translation = translation.name,
            )
            prefs[NOTES_KEY] = json.encodeToString(serializer, updated)
        }
    }

    /** Edits one specific note in place (used from View All Notes). Blank text deletes it. */
    suspend fun updateNote(id: String, text: String) {
        val trimmed = text.trim()
        dataStore.edit { prefs ->
            val current = prefs.readNotes()
            val updated = if (trimmed.isEmpty()) {
                current.filterNot { it.id == id }
            } else {
                current.map { if (it.id == id) it.copy(text = trimmed) else it }
            }
            prefs[NOTES_KEY] = json.encodeToString(serializer, updated)
        }
    }
}
