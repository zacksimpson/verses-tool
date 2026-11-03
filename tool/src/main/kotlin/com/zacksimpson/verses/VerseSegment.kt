package com.zacksimpson.verses

import kotlinx.serialization.Serializable

/**
 * one verse's text plus the layout signals PassageScreen needs: [text] still carries
 * [POETIC_LINE_MARKER]/[POETIC_INDENT_UNIT] for internal poetic line breaks, while
 * [startsNewParagraph] is a separate per-verse flag for prose paragraph breaks, since
 * every source checked always starts a new paragraph exactly on a verse boundary.
 *
 * only meaningful for prose, a poetic verse already gets its own row regardless of
 * this flag.
 */
@Serializable
data class VerseSegment(
    val number: Int,
    val text: String,
    val startsNewParagraph: Boolean = false,
)
