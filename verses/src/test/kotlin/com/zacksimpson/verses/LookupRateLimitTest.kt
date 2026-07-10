package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LookupRateLimitTest {

    @Test
    fun `no stored date means zero count today`() {
        assertEquals(0, LookupRateLimit.countForToday(storedDate = null, storedCount = 42, today = "2026-07-10"))
    }

    @Test
    fun `a stale stored date resets to zero regardless of count`() {
        assertEquals(
            0,
            LookupRateLimit.countForToday(storedDate = "2026-07-09", storedCount = 99, today = "2026-07-10"),
        )
    }

    @Test
    fun `a matching stored date keeps the count`() {
        assertEquals(
            7,
            LookupRateLimit.countForToday(storedDate = "2026-07-10", storedCount = 7, today = "2026-07-10"),
        )
    }

    @Test
    fun `allowed below the limit, blocked at or above it`() {
        assertTrue(
            LookupRateLimit.isAllowed(storedDate = "2026-07-10", storedCount = 99, today = "2026-07-10", limit = 100),
        )
        assertFalse(
            LookupRateLimit.isAllowed(storedDate = "2026-07-10", storedCount = 100, today = "2026-07-10", limit = 100),
        )
        assertFalse(
            LookupRateLimit.isAllowed(storedDate = "2026-07-10", storedCount = 150, today = "2026-07-10", limit = 100),
        )
    }

    @Test
    fun `a new day is always allowed regardless of yesterday's count`() {
        assertTrue(
            LookupRateLimit.isAllowed(storedDate = "2026-07-09", storedCount = 999, today = "2026-07-10", limit = 100),
        )
    }

    @Test
    fun `nextCount increments within the same day and resets across a day change`() {
        assertEquals(6, LookupRateLimit.nextCount(storedDate = "2026-07-10", storedCount = 5, today = "2026-07-10"))
        assertEquals(1, LookupRateLimit.nextCount(storedDate = "2026-07-09", storedCount = 5, today = "2026-07-10"))
        assertEquals(1, LookupRateLimit.nextCount(storedDate = null, storedCount = 0, today = "2026-07-10"))
    }
}
