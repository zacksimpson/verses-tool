package com.zacksimpson.verses

/**
 * Converts VerseCatalog's "Book Chapter:Verse[-Verse]" references into YouVersion
 * passage IDs ("BOOK.chapter.verse" / "BOOK.chapter.verse-verse"). ESV's API takes
 * natural-language references directly, so this is only needed for YouVersion.
 *
 * Range syntax is the short form confirmed against the live API — "EPH.2.8-9", not
 * "EPH.2.8-EPH.2.9" — which matches VerseCatalog anyway since none of its ranges cross
 * a chapter boundary.
 */
internal object UsfmReference {
    private val BOOK_CODES: Map<String, String> = mapOf(
        "Genesis" to "GEN",
        "Exodus" to "EXO",
        "Leviticus" to "LEV",
        "Numbers" to "NUM",
        "Deuteronomy" to "DEU",
        "Joshua" to "JOS",
        "Judges" to "JDG",
        "Ruth" to "RUT",
        "1 Samuel" to "1SA",
        "2 Samuel" to "2SA",
        "1 Kings" to "1KI",
        "2 Kings" to "2KI",
        "1 Chronicles" to "1CH",
        "2 Chronicles" to "2CH",
        "Ezra" to "EZR",
        "Nehemiah" to "NEH",
        "Esther" to "EST",
        "Job" to "JOB",
        "Psalm" to "PSA",
        "Psalms" to "PSA",
        "Proverbs" to "PRO",
        "Ecclesiastes" to "ECC",
        "Song of Solomon" to "SNG",
        "Isaiah" to "ISA",
        "Jeremiah" to "JER",
        "Lamentations" to "LAM",
        "Ezekiel" to "EZK",
        "Daniel" to "DAN",
        "Hosea" to "HOS",
        "Joel" to "JOL",
        "Amos" to "AMO",
        "Obadiah" to "OBA",
        "Jonah" to "JON",
        "Micah" to "MIC",
        "Nahum" to "NAM",
        "Habakkuk" to "HAB",
        "Zephaniah" to "ZEP",
        "Haggai" to "HAG",
        "Zechariah" to "ZEC",
        "Malachi" to "MAL",
        "Matthew" to "MAT",
        "Mark" to "MRK",
        "Luke" to "LUK",
        "John" to "JHN",
        "Acts" to "ACT",
        "Romans" to "ROM",
        "1 Corinthians" to "1CO",
        "2 Corinthians" to "2CO",
        "Galatians" to "GAL",
        "Ephesians" to "EPH",
        "Philippians" to "PHP",
        "Colossians" to "COL",
        "1 Thessalonians" to "1TH",
        "2 Thessalonians" to "2TH",
        "1 Timothy" to "1TI",
        "2 Timothy" to "2TI",
        "Titus" to "TIT",
        "Philemon" to "PHM",
        "Hebrews" to "HEB",
        "James" to "JAS",
        "1 Peter" to "1PE",
        "2 Peter" to "2PE",
        "1 John" to "1JN",
        "2 John" to "2JN",
        "3 John" to "3JN",
        "Jude" to "JUD",
        "Revelation" to "REV",
    )

    private val REFERENCE_PATTERN = Regex("""^(.+?)\s+(\d+):(\d+)(?:-(\d+))?$""")

    fun toPassageId(reference: String): String {
        val match = REFERENCE_PATTERN.matchEntire(reference)
            ?: error("Reference '$reference' doesn't match the expected 'Book Chapter:Verse[-Verse]' format")
        val (bookName, chapter, verse, endVerse) = match.destructured
        val code = bookCode(bookName)
        return if (endVerse.isEmpty()) "$code.$chapter.$verse" else "$code.$chapter.$verse-$endVerse"
    }

    /** Looks up a book name's USFM code directly, for callers that already have a book
     *  name and chapter number rather than a full "Book Chapter:Verse" reference. */
    fun bookCode(bookName: String): String =
        BOOK_CODES[bookName] ?: error("Unknown book name '$bookName'")
}
