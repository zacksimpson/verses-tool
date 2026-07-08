package com.zacksimpson.verses

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** "YYYY-MM-DD" -> "Jan 5, 2026" */
internal fun formatDisplayDate(dateStr: String): String {
    val (y, mo, d) = dateStr.split("-").map(String::toInt)
    return "${MONTHS[mo - 1]} $d, $y"
}
