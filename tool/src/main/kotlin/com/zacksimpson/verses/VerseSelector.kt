package com.zacksimpson.verses

import java.time.LocalDate
import kotlin.random.Random

internal object VerseSelector {
    // the catalog shuffled once with a fixed seed, so a given date always maps to the
    // same verse and no book shows up two days in a row
    private val order: List<String> by lazy { arrange(VerseCatalog.references) }

    fun referenceForDate(date: LocalDate = LocalDate.now()): String =
        order[(date.dayOfYear - 1) % order.size]

    fun bookOf(reference: String): String = reference.substringBeforeLast(' ')

    /** tries seeds until one gives an order with no neighbors from the same book. the
     *  year restarts at the first entry, so day 365 and 366 count as its neighbors too. */
    internal fun arrange(pool: List<String>): List<String> {
        for (seed in 0 until MAX_ATTEMPTS) {
            val attempt = attempt(pool, seed) ?: continue
            val first = bookOf(attempt.first())
            val wraps = listOf(attempt.lastIndex, 364, 365).filter { it in attempt.indices && it > 0 }
            if (wraps.none { bookOf(attempt[it]) == first }) return attempt
        }
        return pool
    }

    private fun attempt(pool: List<String>, seed: Int): List<String>? {
        val remaining = pool.shuffled(Random(seed)).toMutableList()
        val result = mutableListOf<String>()
        while (remaining.isNotEmpty()) {
            val previous = result.lastOrNull()?.let(::bookOf)
            val next = remaining.firstOrNull { bookOf(it) != previous } ?: return null
            remaining.remove(next)
            result.add(next)
        }
        return result
    }

    private const val MAX_ATTEMPTS = 1000
}
