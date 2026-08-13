package com.zacksimpson.verses

/**
 * fetches verse text for whichever [Translation] is active, routed by its
 * [TranslationSource] rather than the translation itself, so a new youversion-backed
 * translation only means a new case in [Translation].
 *
 * backend clients are built lazily so a session that only uses one translation never
 * pays for the others' http clients.
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
        // both public domain providers are free and keyless, always configured
        is TranslationSource.PublicDomain -> true
    }

    fun missingKeyMessage(translation: Translation): String = when (translation.source) {
        is TranslationSource.Esv ->
            "Add your ESV API key to local.properties (esvApiKey=...) to use this tool. " +
                "Get a free key at api.esv.org."
        is TranslationSource.YouVersion ->
            "Add your YouVersion Platform app key to local.properties (youVersionAppKey=...) " +
                "to use this tool. Get a free key at platform.youversion.com."
        // isConfigured() is always true here, so this message never actually shows
        is TranslationSource.PublicDomain -> ""
    }

    suspend fun fetchVerseText(translation: Translation, reference: String): Result<String> =
        when (val source = translation.source) {
            is TranslationSource.Esv -> esvApi.fetchVerseText(reference)
            is TranslationSource.YouVersion -> youVersionApi.fetchVerseText(source.versionId, reference)
            is TranslationSource.PublicDomain -> helloAoApi(source.provider).fetchVerseText(reference)
        }

    /** same passage as [fetchVerseText] but keeps each verse's number and paragraph
     *  structure instead of flattening it, used by the passage screen. */
    suspend fun fetchVerses(translation: Translation, reference: String): Result<List<VerseSegment>> =
        when (val source = translation.source) {
            is TranslationSource.PublicDomain -> helloAoApi(source.provider).fetchVerses(reference)
            is TranslationSource.Esv -> esvApi.fetchVerses(reference)
            is TranslationSource.YouVersion -> youVersionApi.fetchVerses(source.versionId, reference)
        }

    /** only public domain text has no rate limit or storage restriction to protect, so
     *  it's the only source a whole chapter can be fetched from. esv and youversion fail
     *  outright here, no continuous chapter browsing in copyrighted translations. */
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

    /** the whole chapter [book] [chapter], for jumping straight from a reference into
     *  full-chapter reading. public domain sources use the free [fetchChapter]; copyrighted
     *  sources go through the same rate-limited single-passage lookup a manual search uses,
     *  since free chapter browsing is public domain only. */
    suspend fun fetchWholeChapter(translation: Translation, book: String, chapter: Int): Result<List<VerseSegment>> =
        when (translation.source) {
            is TranslationSource.PublicDomain -> fetchChapter(translation, book, chapter).map { it.verses }
            is TranslationSource.Esv, is TranslationSource.YouVersion -> {
                val verseCount = BibleBooks.all.first { it.name == book }.versesPerChapter[chapter - 1]
                fetchVerses(translation, "$book $chapter:1-$verseCount")
            }
        }

    fun close() {
        if (esvApiLazy.isInitialized()) esvApi.close()
        if (youVersionApiLazy.isInitialized()) youVersionApi.close()
        if (helloAoKjvLazy.isInitialized()) helloAoKjvApi.close()
        if (helloAoBsbLazy.isInitialized()) helloAoBsbApi.close()
    }
}
