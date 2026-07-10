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
    private val bibleApiComLazy = lazy { BibleApiComApi() }
    private val esvApi by esvApiLazy
    private val youVersionApi by youVersionApiLazy
    private val bibleApiComApi by bibleApiComLazy

    fun isConfigured(translation: Translation): Boolean = when (translation.source) {
        is TranslationSource.Esv -> BuildConfig.ESV_API_KEY.isNotBlank()
        is TranslationSource.YouVersion -> BuildConfig.YOUVERSION_APP_KEY.isNotBlank()
        // bible-api.com is free and keyless — always configured.
        is TranslationSource.PublicDomain -> true
    }

    fun missingKeyMessage(translation: Translation): String = when (translation.source) {
        is TranslationSource.Esv ->
            "Add your ESV API key to local.properties (esvApiKey=...) to use this tool. " +
                "Get a free key at api.esv.org."
        is TranslationSource.YouVersion ->
            "Add your YouVersion Platform app key to local.properties (youVersionAppKey=...) " +
                "to use this tool. Get a free key at platform.youversion.com."
        // isConfigured() is always true for this source, so this is never actually shown.
        is TranslationSource.PublicDomain -> ""
    }

    suspend fun fetchVerseText(translation: Translation, reference: String): Result<String> =
        when (val source = translation.source) {
            is TranslationSource.Esv -> esvApi.fetchVerseText(reference)
            is TranslationSource.YouVersion -> youVersionApi.fetchVerseText(source.versionId, reference)
            is TranslationSource.PublicDomain -> bibleApiComApi.fetchVerseText(reference)
        }

    /** Only public domain text has no rate limit or storage restriction to protect, so
     *  this is the only source a whole chapter may be fetched from — Esv/YouVersion fail
     *  outright rather than silently fetching, enforcing "no continuous chapter browsing
     *  in copyrighted translations" here regardless of what UI calls this. */
    suspend fun fetchChapter(translation: Translation, book: String, chapter: Int): Result<BibleChapter> =
        when (translation.source) {
            is TranslationSource.PublicDomain -> bibleApiComApi.fetchChapter(book, chapter)
            is TranslationSource.Esv, is TranslationSource.YouVersion ->
                Result.failure(
                    UnsupportedOperationException(
                        "Chapter browsing isn't available for ${translation.abbreviation}; " +
                            "only single-passage lookup is.",
                    ),
                )
        }

    fun close() {
        if (esvApiLazy.isInitialized()) esvApi.close()
        if (youVersionApiLazy.isInitialized()) youVersionApi.close()
        if (bibleApiComLazy.isInitialized()) bibleApiComApi.close()
    }
}
