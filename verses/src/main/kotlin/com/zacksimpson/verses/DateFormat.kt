package com.zacksimpson.verses

import java.time.LocalDate

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** "YYYY-MM-DD" -> "Jan 5, 2026" */
internal fun formatDisplayDate(dateStr: String): String {
    val (y, mo, d) = dateStr.split("-").map(String::toInt)
    return "${MONTHS[mo - 1]} $d, $y"
}

/** "YYYY-MM-DD" -> "Jan 5", or "Jan 5, 2026" once that date is over a year old. */
internal fun formatRelativeDate(dateStr: String): String {
    val (y, mo, d) = dateStr.split("-").map(String::toInt)
    val date = LocalDate.of(y, mo, d)
    val overAYearOld = date.isBefore(LocalDate.now().minusYears(1))
    return if (overAYearOld) formatDisplayDate(dateStr) else "${MONTHS[mo - 1]} $d"
}
