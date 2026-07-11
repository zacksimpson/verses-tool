package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertEquals

class BibleBooksTest {

    @Test
    fun `has all 66 books`() {
        assertEquals(66, BibleBooks.all.size)
    }

    @Test
    fun `every book name is recognized by UsfmReference`() {
        // toPassageId throws on an unknown book name — this indirectly checks every
        // name here matches UsfmReference's own book list, without needing access to
        // its private map.
        for (book in BibleBooks.all) {
            UsfmReference.toPassageId("${book.name} 1:1")
        }
    }

    @Test
    fun `every chapter count is positive`() {
        for (book in BibleBooks.all) {
            assert(book.chapterCount > 0) { "${book.name} has a non-positive chapter count" }
        }
    }

    @Test
    fun `splits into 39 Old Testament and 27 New Testament books`() {
        assertEquals(39, BibleBooks.all.count { it.testament == Testament.OLD })
        assertEquals(27, BibleBooks.all.count { it.testament == Testament.NEW })
    }

    @Test
    fun `every chapter has a matching verse count entry and totals sensibly`() {
        for (book in BibleBooks.all) {
            assertEquals(book.chapterCount, book.versesPerChapter.size, "${book.name} chapter/verse-count mismatch")
        }
        val totalChapters = BibleBooks.all.sumOf { it.chapterCount }
        val totalVerses = BibleBooks.all.sumOf { it.versesPerChapter.sum() }
        assertEquals(1189, totalChapters)
        assertEquals(31086, totalVerses)
    }

    @Test
    fun `spot-checks a few well-known chapter lengths`() {
        fun versesIn(book: String, chapter: Int) = BibleBooks.all.first { it.name == book }.versesPerChapter[chapter - 1]

        assertEquals(31, versesIn("Genesis", 1))
        assertEquals(36, versesIn("John", 3))
        assertEquals(2, versesIn("Psalm", 117)) // shortest chapter in the Bible
        assertEquals(176, versesIn("Psalm", 119)) // longest chapter in the Bible
    }
}
