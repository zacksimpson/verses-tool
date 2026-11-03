package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertEquals

// Fixtures below are real responses captured live from api.esv.org (include-verse-numbers=
// true, indent-poetry-lines=4, indent-paragraphs=2) — not hand-constructed — so these tests
// pin down the actual raw format, not an assumption about it.
class EsvTextParsingTest {

    @Test
    fun `a single flowing paragraph keeps its verses as continuations, not new paragraphs`() {
        val raw = "  [16] “For God so loved the world, that he gave his only Son, that whoever " +
            "believes in him should not perish but have eternal life. [17] For God did not send " +
            "his Son into the world to condemn the world, but in order that the world might be " +
            "saved through him. [18] Whoever believes in him is not condemned, but whoever does " +
            "not believe is condemned already, because he has not believed in the name of the " +
            "only Son of God.\n\n"

        val verses = EsvTextParsing.versesFromPassageText(raw)

        assertEquals(listOf(16, 17, 18), verses.map { it.number })
        assertEquals(false, verses[1].startsNewParagraph, "verse 17 continues verse 16's paragraph")
        assertEquals(false, verses[2].startsNewParagraph, "verse 18 continues the same paragraph")
        assertEquals(
            "For God did not send his Son into the world to condemn the world, but in order " +
                "that the world might be saved through him.",
            verses[1].text,
        )
        for (verse in verses) assertEquals(false, isPoeticText(verse.text))
    }

    @Test
    fun `poetry decodes each verse into its own marked, indented lines`() {
        val raw = "A Psalm of David.\n\n    [1] The LORD is my shepherd; I shall not want.\n" +
            "    [2]     He makes me lie down in green pastures.\n    He leads me beside still waters.\n" +
            "    [3]     He restores my soul.\n    He leads me in paths of righteousness\n" +
            "        for his name’s sake.\n    \n    \n" +
            "    [4] Even though I walk through the valley of the shadow of death,\n" +
            "        I will fear no evil,\n    for you are with me;\n        your rod and your staff,\n" +
            "        they comfort me.\n    \n    \n" +
            "    [5] You prepare a table before me\n        in the presence of my enemies;\n" +
            "    you anoint my head with oil;\n        my cup overflows.\n" +
            "    [6] Surely goodness and mercy shall follow me\n        all the days of my life,\n" +
            "    and I shall dwell in the house of the LORD\n        forever.\n    \n\n"

        val verses = EsvTextParsing.versesFromPassageText(raw)

        assertEquals(listOf(1, 2, 3, 4, 5, 6), verses.map { it.number })
        assertEquals("The LORD is my shepherd; I shall not want.", verses[0].text)
        assertEquals(false, isPoeticText(verses[0].text), "verse 1 is a single unindented line")

        assertEquals(
            listOf(
                VerseLine(0, "He makes me lie down in green pastures."),
                VerseLine(0, "He leads me beside still waters."),
            ),
            linesFromVerseText(verses[1].text),
        )

        assertEquals(
            listOf(
                VerseLine(0, "He restores my soul."),
                VerseLine(0, "He leads me in paths of righteousness"),
                VerseLine(1, "for his name’s sake."),
            ),
            linesFromVerseText(verses[2].text),
        )
    }

    @Test
    fun `a blank-line stanza break between verses does not leak into either verse's text`() {
        val raw = "    [8] For my thoughts are not your thoughts,\n" +
            "        neither are your ways my ways, declares the LORD.\n" +
            "    [9] For as the heavens are higher than the earth,\n" +
            "        so are my ways higher than your ways\n" +
            "        and my thoughts than your thoughts.\n    \n    \n" +
            "    [10] “For as the rain and the snow come down from heaven\n" +
            "        and do not return there but water the earth,\n"

        val verses = EsvTextParsing.versesFromPassageText(raw)

        assertEquals(listOf(8, 9, 10), verses.map { it.number })
        assertEquals(
            listOf(
                VerseLine(0, "For as the heavens are higher than the earth,"),
                VerseLine(1, "so are my ways higher than your ways"),
                VerseLine(1, "and my thoughts than your thoughts."),
            ),
            linesFromVerseText(verses[1].text),
        )
    }
}
