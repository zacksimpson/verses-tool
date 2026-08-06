package com.zacksimpson.verses

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed class VerseUiState {
    data object Loading : VerseUiState()
    data class Loaded(
        val reference: String,
        val text: String,
        val translation: Translation,
        val staleWarning: Boolean = false,
    ) : VerseUiState()
    data class ConfigError(val message: String) : VerseUiState()
}

class VersesViewModel(
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {
    private val fetcher = VerseFetcher()

    private val _uiState = MutableStateFlow<VerseUiState>(VerseUiState.Loading)
    val uiState: StateFlow<VerseUiState> = _uiState.asStateFlow()

    // paints the cache immediately if there is one, staleness check happens in
    // onScreenShow below. onScreenShow joins this job first so the two coroutines don't
    // race and overwrite a fresh fetch with a stale paint
    private val initialLoadJob: Job = viewModelScope.launch(Dispatchers.IO) {
        showCachedStateIfAvailable()
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        viewModelScope.launch(Dispatchers.IO) {
            initialLoadJob.join()
            refreshIfStale(dataStore.data.first())
        }
    }

    private suspend fun showCachedStateIfAvailable() {
        val prefs = dataStore.data.first()
        val translation = prefs.selectedTranslation()
        val cachedRef = prefs[VersePreferences.CACHED_REFERENCE]
        val cachedText = prefs[VersePreferences.CACHED_TEXT]

        // only show the cache if it matches the current translation, a mismatch gets
        // resolved by refreshIfStale instead of flashing the old translation's text
        if (cachedRef != null && cachedText != null && prefs.cachedTranslation() == translation) {
            setState(VerseUiState.Loaded(reference = cachedRef, text = cachedText, translation = translation))
        }
    }

    private suspend fun refreshIfStale(prefs: Preferences) {
        val translation = prefs.selectedTranslation()
        val cachedTranslation = prefs.cachedTranslation()
        val cachedDate = prefs[VersePreferences.CACHED_DATE]
        val existingRef = prefs[VersePreferences.CACHED_REFERENCE]
        val existingText = prefs[VersePreferences.CACHED_TEXT]

        val today = LocalDate.now()
        val todayIso = today.toString()

        if (cachedDate == todayIso && cachedTranslation == translation) return

        if (!fetcher.isConfigured(translation)) {
            showCachedOrError(existingRef, existingText, cachedTranslation, fetcher.missingKeyMessage(translation))
            return
        }

        val reference = VerseSelector.referenceForDate(today)
        val result = fetcher.fetchVerseText(translation, reference)
        result.fold(
            onSuccess = { text ->
                dataStore.edit { p ->
                    p[VersePreferences.CACHED_DATE] = todayIso
                    p[VersePreferences.CACHED_REFERENCE] = reference
                    p[VersePreferences.CACHED_TEXT] = text
                    p[VersePreferences.CACHED_TRANSLATION] = translation.name
                }
                // counted here too, not just manual lookups, so API Logs reflects every
                // real call. no-op for public domain translations
                LookupRateLimiter(dataStore).recordLookup(translation)
                setState(VerseUiState.Loaded(reference = reference, text = text, translation = translation))
            },
            onFailure = {
                showCachedOrError(existingRef, existingText, cachedTranslation, "Couldn't load today's verse. Try again shortly.")
            },
        )
    }

    /** falls back to whatever is cached instead of a bare error, labeled with
     *  [cachedTranslation] (what's actually in the cache) so it never gets mislabeled
     *  after a translation switch. */
    private suspend fun showCachedOrError(
        existingRef: String?,
        existingText: String?,
        cachedTranslation: Translation,
        errorMessage: String,
    ) {
        if (existingRef != null && existingText != null) {
            setState(VerseUiState.Loaded(existingRef, existingText, translation = cachedTranslation, staleWarning = true))
        } else {
            setState(VerseUiState.ConfigError(errorMessage))
        }
    }

    private suspend fun setState(state: VerseUiState) {
        withContext(Dispatchers.Main) { _uiState.value = state }
    }

    override fun onCleared() {
        super.onCleared()
        fetcher.close()
    }
}
