package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiLogsScreenTest {

    @Test
    fun `formatAgainstLimit shows the value alone when no limit applies`() {
        assertEquals("8", formatAgainstLimit(8, limit = null))
    }

    @Test
    fun `formatAgainstLimit shows value over limit when one applies`() {
        assertEquals("12 / 100", formatAgainstLimit(12, limit = DAILY_LOOKUP_LIMIT))
    }
}
