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
    // nullable so notes stored before this field existed still deserialize, read via
    // resolvedTranslation() below, not directly
    val translation: String? = null,
)

/** notes predating this field don't know their translation, same fallback as
 *  Translation.fromNameOrDefault since VerseNote stores the raw name. */
fun VerseNote.resolvedTranslation(): Translation = Translation.fromNameOrDefault(translation)

private val NOTES_KEY = stringPreferencesKey("verse_notes")

/**
 * free-text notes on a verse-of-the-day date, keyed by date rather than reference so a
 * repeated reference in a future year gets its own notes instead of reusing old ones.
 * a date can have multiple notes, add notes always appends.
 */
class VerseNotesRepository(private val dataStore: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(VerseNote.serializer())

    val notes: Flow<List<VerseNote>> = dataStore.data.map { prefs -> prefs.readNotes() }

    private fun Preferences.readNotes(): List<VerseNote> =
        this[NOTES_KEY]?.let { raw -> runCatching { json.decodeFromString(serializer, raw) }.getOrNull() }
            ?: emptyList()

    /** always appends a new note, blank text is a no-op. */
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

    /** edits one note in place (from all notes). blank text deletes it. */
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
