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
}
