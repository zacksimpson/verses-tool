package com.zacksimpson.verses

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
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
        val staleWarning: Boolean = false,
    ) : VerseUiState()
    data class ConfigError(val message: String) : VerseUiState()
}

class VersesViewModel(
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {
    private val api = EsvApi(apiKey = BuildConfig.ESV_API_KEY)

    private val _uiState = MutableStateFlow<VerseUiState>(VerseUiState.Loading)
    val uiState: StateFlow<VerseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadStoredState()
        }
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        viewModelScope.launch(Dispatchers.IO) {
            refreshIfStale()
        }
    }

    private suspend fun loadStoredState() {
        val prefs = dataStore.data.first()
        val cachedRef = prefs[VersePreferences.CACHED_REFERENCE]
        val cachedText = prefs[VersePreferences.CACHED_TEXT]

        if (cachedRef != null && cachedText != null) {
            setState(VerseUiState.Loaded(reference = cachedRef, text = cachedText))
        }
        refreshIfStale()
    }

    private suspend fun refreshIfStale() {
        if (BuildConfig.ESV_API_KEY.isBlank()) {
            setState(
                VerseUiState.ConfigError(
                    "Add your ESV API key to local.properties (esvApiKey=...) to use this tool. " +
                        "Get a free key at api.esv.org.",
                ),
            )
            return
        }

        val today = LocalDate.now()
        val todayIso = today.toString()
        val prefs = dataStore.data.first()
        val cachedDate = prefs[VersePreferences.CACHED_DATE]

        if (cachedDate == todayIso) return

        val reference = VerseSelector.referenceForDate(today)
        val result = api.fetchVerseText(reference)
        result.fold(
            onSuccess = { text ->
                dataStore.edit { p ->
                    p[VersePreferences.CACHED_DATE] = todayIso
                    p[VersePreferences.CACHED_REFERENCE] = reference
                    p[VersePreferences.CACHED_TEXT] = text
                }
                setState(VerseUiState.Loaded(reference = reference, text = text))
            },
            onFailure = {
                val existingRef = prefs[VersePreferences.CACHED_REFERENCE]
                val existingText = prefs[VersePreferences.CACHED_TEXT]
                if (existingRef != null && existingText != null) {
                    setState(VerseUiState.Loaded(existingRef, existingText, staleWarning = true))
                } else {
                    setState(VerseUiState.ConfigError("Couldn't load today's verse. Check your connection."))
                }
            },
        )
    }

    private suspend fun setState(state: VerseUiState) {
        withContext(Dispatchers.Main) { _uiState.value = state }
    }

    override fun onCleared() {
        super.onCleared()
        api.close()
    }
}
