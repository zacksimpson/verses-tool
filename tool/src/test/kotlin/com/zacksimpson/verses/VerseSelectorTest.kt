package com.zacksimpson.verses

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VerseSelectorTest {

    private val pool = VerseCatalog.references

    private fun parse(reference: String): Triple<String, Int, IntRange> {
        val book = VerseSelector.bookOf(reference)
        val (chapter, verses) = reference.substringAfterLast(' ').split(':')
        val range = verses.split('-').map { it.toInt() }
        return Triple(book, chapter.toInt(), range.first()..range.last())
    }

    @Test
    fun `catalog covers a full leap year`() {
        assertTrue(pool.size >= 366, "only ${pool.size} verses")
    }

    @Test
    fun `every reference exists and stays inside one chapter`() {
        for (reference in pool) {
            val (book, chapter, verses) = parse(reference)
            val counts = BibleBooks.all.first { it.name == book }.versesPerChapter
            assertTrue(chapter in 1..counts.size, reference)
            assertTrue(verses.first >= 1 && verses.last <= counts[chapter - 1], reference)
        }
    }

    @Test
    fun `no two references overlap`() {
        val parsed = pool.map(::parse)
        for (i in parsed.indices) for (j in i + 1 until parsed.size) {
            val (b1, c1, v1) = parsed[i]
            val (b2, c2, v2) = parsed[j]
            if (b1 == b2 && c1 == c2) {
                assertTrue(v1.last < v2.first || v2.last < v1.first, "${pool[i]} overlaps ${pool[j]}")
            }
        }
    }

    @Test
    fun `a leap year never repeats a verse`() {
        val year = generateSequence(LocalDate.of(2028, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year == 2028 }
            .map { VerseSelector.referenceForDate(it) }
            .toList()
        assertEquals(366, year.size)
        assertEquals(366, year.toSet().size)
    }

    @Test
    fun `the same book never shows two days in a row`() {
        val year = generateSequence(LocalDate.of(2026, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year < 2030 }
            .map { VerseSelector.bookOf(VerseSelector.referenceForDate(it)) }
            .toList()
        year.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
    }

    @Test
    fun `a date always gets the same verse`() {
        val day = LocalDate.of(2026, 7, 4)
        assertEquals(VerseSelector.referenceForDate(day), VerseSelector.referenceForDate(day))
    }
}
