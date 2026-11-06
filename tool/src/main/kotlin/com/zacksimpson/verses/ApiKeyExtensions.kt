package com.zacksimpson.verses

import androidx.datastore.preferences.core.Preferences

/** the key a translation needs, null for the keyless public domain ones. */
internal fun Translation.apiKeyService(): ApiKeyService? = when (source) {
    is TranslationSource.Esv -> ApiKeyService.Esv
    is TranslationSource.YouVersion -> ApiKeyService.YouVersion
    is TranslationSource.PublicDomain -> null
}

internal fun ApiKeyService.isSetIn(prefs: Preferences): Boolean =
    !prefs[preferenceKey].isNullOrBlank()
