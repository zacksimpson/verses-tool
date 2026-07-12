package com.zacksimpson.verses

/**
 * Fetches verse text for whichever [Translation] is active, routing on its
 * [TranslationSource] rather than the translation itself — adding a new YouVersion-backed
 * translation only means adding a case to [Translation], not touching this file.
 *
 * All backend clients are constructed lazily so a session that only ever uses one
 * translation (the common case) never pays for the others' HTTP clients — including the
 * two public domain ones, which are separate HelloAoApi instances (one per translation id)
 * even though they hit the same host.
 */
internal class VerseFetcher {
    private val esvApiLazy = lazy { EsvApi(apiKey = BuildConfig.ESV_API_KEY) }
    private val youVersionApiLazy = lazy { YouVersionApi(appKey = BuildConfig.YOUVERSION_APP_KEY) }
    private val helloAoKjvLazy = lazy { HelloAoApi(PublicDomainProvider.KJV.translationId) }
    private val helloAoBsbLazy = lazy { HelloAoApi(PublicDomainProvider.BSB.translationId) }
    private val esvApi by esvApiLazy
    private val youVersionApi by youVersionApiLazy
    private val helloAoKjvApi by helloAoKjvLazy
    private val helloAoBsbApi by helloAoBsbLazy

    private fun helloAoApi(provider: PublicDomainProvider) = when (provider) {
        PublicDomainProvider.KJV -> helloAoKjvApi
        PublicDomainProvider.BSB -> helloAoBsbApi
    }

    fun isConfigured(translation: Translation): Boolean = when (translation.source) {
        is TranslationSource.Esv -> BuildConfig.ESV_API_KEY.isNotBlank()
        is TranslationSource.YouVersion -> BuildConfig.YOUVERSION_APP_KEY.isNotBlank()
        // Both public domain providers are free and keyless — always configured.
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
            is TranslationSource.PublicDomain -> helloAoApi(source.provider).fetchVerseText(reference)
        }

    /** Same passage as [fetchVerseText], but with each verse's number kept alongside its
     *  text instead of flattened into one string — used by the passage display screen so
     *  a multi-verse selection can show its verse numbers. Public domain sources return
     *  true per-verse structure (their own APIs already provide it); ESV/YouVersion have
     *  no structured multi-verse endpoint without either parsing verse markers out of
     *  ESV's own text format or making one request per verse (which would multiply the
     *  call count) — so for those, the whole range comes back as a single block labeled
     *  with the range's starting verse rather than true per-verse numbers. */
    suspend fun fetchVerses(translation: Translation, reference: String): Result<List<Pair<Int, String>>> =
        when (val source = translation.source) {
            is TranslationSource.PublicDomain -> helloAoApi(source.provider).fetchVerses(reference)
            is TranslationSource.Esv, is TranslationSource.YouVersion -> fetchVerseText(translation, reference).mapCatching { text ->
                val startVerse = UsfmReference.parseRange(UsfmReference.toPassageId(reference)).startVerse
                listOf(startVerse to text)
            }
        }

    /** Only public domain text has no rate limit or storage restriction to protect, so
     *  this is the only source a whole chapter may be fetched from — Esv/YouVersion fail
     *  outright rather than silently fetching, enforcing "no continuous chapter browsing
     *  in copyrighted translations" here regardless of what UI calls this. */
    suspend fun fetchChapter(translation: Translation, book: String, chapter: Int): Result<BibleChapter> =
        when (val source = translation.source) {
            is TranslationSource.PublicDomain -> helloAoApi(source.provider).fetchChapter(book, chapter)
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
        if (helloAoKjvLazy.isInitialized()) helloAoKjvApi.close()
        if (helloAoBsbLazy.isInitialized()) helloAoBsbApi.close()
    }
}
