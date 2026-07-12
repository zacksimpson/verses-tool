package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertEquals

// Fixtures below are real format=html responses captured live from api.youversion.com for
// NIV (111) and NASB (2692) — not hand-constructed — so these tests pin down the actual raw
// markup, not an assumption about it.
class YouVersionHtmlParsingTest {

    @Test
    fun `NIV Psalm 23 decodes each verse's poetic lines at their own indent levels`() {
        val html = "<div><div class=\"d\">A psalm of David.</div>" +
            "<div class=\"q1\"><span class=\"yv-v\" v=\"1\"></span><span class=\"yv-vlbl\">1</span>The " +
            "<span class=\"nd\">Lord</span> is my shepherd, I lack nothing.</div>" +
            "<div class=\"q2\"><span class=\"yv-v\" v=\"2\"></span><span class=\"yv-vlbl\">2</span>He makes me lie down in green pastures,</div>" +
            "<div class=\"q1\">he leads me beside quiet waters,</div>" +
            "<div class=\"q2\"><span class=\"yv-v\" v=\"3\"></span><span class=\"yv-vlbl\">3</span>he refreshes my soul.</div>" +
            "<div class=\"q1\">He guides me along the right paths</div>" +
            "<div class=\"q2\">for his name’s sake.</div></div>"

        val verses = YouVersionHtmlParsing.versesFromHtml(html)

        assertEquals(listOf(1, 2, 3), verses.map { it.number })
        assertEquals(listOf(VerseLine(0, "The Lord is my shepherd, I lack nothing.")), linesFromVerseText(verses[0].text))
        assertEquals(false, verses[0].startsNewParagraph, "poetic verses don't need the paragraph flag")

        assertEquals(
            listOf(
                VerseLine(1, "He makes me lie down in green pastures,"),
                VerseLine(0, "he leads me beside quiet waters,"),
            ),
            linesFromVerseText(verses[1].text),
        )
        assertEquals(
            listOf(
                VerseLine(1, "he refreshes my soul."),
                VerseLine(0, "He guides me along the right paths"),
                VerseLine(1, "for his name’s sake."),
            ),
            linesFromVerseText(verses[2].text),
        )
    }

    @Test
    fun `NIV John 3 groups all three verses of one paragraph, only the first starting a new paragraph`() {
        val html = "<div><div class=\"p\">" +
            "<span class=\"yv-v\" v=\"16\"></span><span class=\"yv-vlbl\">16</span>For God so loved the world " +
            "that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life. " +
            "<span class=\"yv-v\" v=\"17\"></span><span class=\"yv-vlbl\">17</span>For God did not send his Son " +
            "into the world to condemn the world, but to save the world through him. " +
            "<span class=\"yv-v\" v=\"18\"></span><span class=\"yv-vlbl\">18</span>Whoever believes in him is not " +
            "condemned, but whoever does not believe stands condemned already because they have not believed in " +
            "the name of God’s one and only Son. </div></div>"

        val verses = YouVersionHtmlParsing.versesFromHtml(html)

        assertEquals(listOf(16, 17, 18), verses.map { it.number })
        assertEquals(true, verses[0].startsNewParagraph)
        assertEquals(false, verses[1].startsNewParagraph)
        assertEquals(false, verses[2].startsNewParagraph)
        assertEquals(false, isPoeticText(verses[0].text))
        assertEquals(
            "For God did not send his Son into the world to condemn the world, but to save the world through him.",
            verses[1].text,
        )
    }

    @Test
    fun `NASB's single unleveled q class and stray pilcrow are handled`() {
        val html = "<div><div class=\"d\">A Psalm of David.</div>" +
            "<div class=\"q\"><span class=\"yv-v\" v=\"1\"></span><span class=\"yv-vlbl\">1</span>The L<span class=\"sc\">ord</span> is my shepherd,</div>" +
            "<div class=\"q\">I will not be in need.</div>" +
            "<div class=\"q\"><span class=\"yv-v\" v=\"4\"></span><span class=\"yv-vlbl\">4</span>¶Even though I walk through the valley of the shadow of death,</div>" +
            "<div class=\"q\">I fear no evil, for You are with me;</div></div>"

        val verses = YouVersionHtmlParsing.versesFromHtml(html)

        assertEquals(listOf(1, 4), verses.map { it.number })
        assertEquals(
            listOf(
                VerseLine(0, "The Lord is my shepherd,"),
                VerseLine(0, "I will not be in need."),
            ),
            linesFromVerseText(verses[0].text),
        )
        assertEquals(
            listOf(
                VerseLine(0, "Even though I walk through the valley of the shadow of death,"),
                VerseLine(0, "I fear no evil, for You are with me;"),
            ),
            linesFromVerseText(verses[1].text),
        )
    }
}
