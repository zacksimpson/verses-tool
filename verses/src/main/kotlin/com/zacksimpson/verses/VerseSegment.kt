package com.zacksimpson.verses

/**
 * One verse's text plus the structural signals PassageScreen needs to lay it out:
 * [text] itself still carries [POETIC_LINE_MARKER]/[POETIC_INDENT_UNIT] for any internal
 * poetic line breaks, while [startsNewParagraph] is a separate, verse-boundary-granularity
 * flag for standard prose paragraph/section breaks — every source checked (ESV, BSB,
 * YouVersion) always starts a new paragraph exactly on a verse boundary, never mid-verse,
 * so a per-verse flag is enough; no in-string marker needed for it.
 *
 * Only meaningful for prose: a poetic verse already gets its own row in PassageScreen's
 * rowGroupsFor regardless of this flag, so it's harmless (if often true) on poetic verses.
 */
data class VerseSegment(
    val number: Int,
    val text: String,
    val startsNewParagraph: Boolean = false,
)
