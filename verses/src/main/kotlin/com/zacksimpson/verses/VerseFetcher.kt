package com.zacksimpson.verses

/**
 * Fetches verse text for whichever [Translation] is active, routing on its
 * [TranslationSource] rather than the translation itself — adding a new YouVersion-backed
 * translation only means adding a case to [Translation], not touching this file.
 *
 * Both backend clients are constructed lazily so a session that only ever uses one
 * translation (the common case) never pays for the other's HTTP client.
 */
internal class VerseFetcher {
    private val esvApiLazy = lazy { EsvApi(apiKey = BuildConfig.ESV_API_KEY) }
    private val youVersionApiLazy = lazy { YouVersionApi(appKey = BuildConfig.YOUVERSION_APP_KEY) }
    private val esvApi by esvApiLazy
    private val youVersionApi by youVersionApiLazy

    fun isConfigured(translation: Translation): Boolean = when (translation.source) {
        is TranslationSource.Esv -> BuildConfig.ESV_API_KEY.isNotBlank()
        is TranslationSource.YouVersion -> BuildConfig.YOUVERSION_APP_KEY.isNotBlank()
    }

    fun missingKeyMessage(translation: Translation): String = when (translation.source) {
        is TranslationSource.Esv ->
            "Add your ESV API key to local.properties (esvApiKey=...) to use this tool. " +
                "Get a free key at api.esv.org."
        is TranslationSource.YouVersion ->
            "Add your YouVersion Platform app key to local.properties (youVersionAppKey=...) " +
                "to use this tool. Get a free key at platform.youversion.com."
    }

    suspend fun fetchVerseText(translation: Translation, reference: String): Result<String> =
        when (val source = translation.source) {
            is TranslationSource.Esv -> esvApi.fetchVerseText(reference)
            is TranslationSource.YouVersion -> youVersionApi.fetchVerseText(source.versionId, reference)
        }

    fun close() {
        if (esvApiLazy.isInitialized()) esvApi.close()
        if (youVersionApiLazy.isInitialized()) youVersionApi.close()
    }
}
