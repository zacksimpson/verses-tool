package com.zacksimpson.verses

/** which backend a [Translation] is fetched through, lets [VerseFetcher] branch on the
 *  api shape instead of each individual translation, so a new youversion-backed
 *  translation only means a new enum case below. also carries the caching note shown in
 *  copyright info, since that's a property of the backend, not any one translation.
 */
sealed interface TranslationSource {
    val usageNote: String

    data object Esv : TranslationSource {
        override val usageNote = "Verse text is fetched from the official ESV API " +
            "(api.esv.org) once per day and cached on-device so the tool works offline; only the " +
            "current day's verse is kept, and it's replaced the next time a new verse is fetched, " +
            "under Crossway's terms for free, non-commercial use."
    }

    data class YouVersion(val versionId: Int) : TranslationSource {
        override val usageNote = "Verse text is fetched through the YouVersion Platform, under the " +
            "publisher's terms for free, non-commercial use, once per day and cached on-device the " +
            "same way as above."
    }

    /** public domain text, unlike esv/youversion has no rate limit or storage
     *  restriction, so it's the only source that supports fetching a whole chapter.
     *  both providers come from bible.helloao.org under different translation ids. */
    data class PublicDomain(val provider: PublicDomainProvider) : TranslationSource {
        override val usageNote = provider.usageNote
    }
}

enum class PublicDomainProvider(val translationId: String, val usageNote: String) {
    KJV(
        translationId = "eng_kjv",
        usageNote = "Verse text is fetched from bible.helloao.org, a free service hosting " +
            "the King James Version. No usage restrictions apply.",
    ),
    BSB(
        translationId = "BSB",
        usageNote = "Verse text is fetched from bible.helloao.org, a free service hosting " +
            "the Berean Standard Bible. No usage restrictions apply.",
    ),
}

enum class Translation(
    val abbreviation: String,
    val displayName: String,
    val source: TranslationSource,
    val copyrightNotice: String,
    val trademarkNotice: String,
) {
    ESV(
        abbreviation = "ESV",
        displayName = "English Standard Version",
        source = TranslationSource.Esv,
        copyrightNotice = "Scripture quotations are from the ESV® Bible " +
            "(The Holy Bible, English Standard Version®), copyright © 2001 by Crossway, a publishing " +
            "ministry of Good News Publishers. Used by permission. All rights reserved.",
        trademarkNotice = "\"ESV\" and \"English Standard Version\" are registered trademarks of Crossway.",
    ),
    NIV(
        abbreviation = "NIV",
        displayName = "New International Version",
        source = TranslationSource.YouVersion(versionId = 111),
        copyrightNotice = "Scripture quotations taken from The Holy Bible, New " +
            "International Version® NIV® Copyright © 1973, 1978, 1984, 2011 by Biblica, Inc.™ Used by " +
            "permission. All rights reserved worldwide.",
        trademarkNotice = "\"NIV\" and \"New International Version\" are trademarks registered in the " +
            "United States Patent and Trademark Office by Biblica, Inc.",
    ),
    NASB(
        abbreviation = "NASB",
        displayName = "New American Standard Bible",
        source = TranslationSource.YouVersion(versionId = 2692),
        copyrightNotice = "Scripture quotations taken from the (NASB®) New " +
            "American Standard Bible®, Copyright © 1960, 1971, 1977, 1995, 2020 by The Lockman " +
            "Foundation. Used by permission. All rights reserved. www.Lockman.org",
        trademarkNotice = "\"NASB\" and \"New American Standard Bible\" are registered trademarks of " +
            "The Lockman Foundation.",
    ),
    KJV(
        abbreviation = "KJV",
        displayName = "King James Version",
        source = TranslationSource.PublicDomain(PublicDomainProvider.KJV),
        copyrightNotice = "The King James Version (Authorized Version) of the Bible is in the " +
            "public domain in the United States.",
        trademarkNotice = "The King James Version has no trademark or copyright holder; it is " +
            "freely available for any use.",
    ),
    BSB(
        abbreviation = "BSB",
        displayName = "Berean Standard Bible",
        source = TranslationSource.PublicDomain(PublicDomainProvider.BSB),
        copyrightNotice = "The Berean Standard Bible was dedicated to the public domain by its " +
            "publisher, Bible Hub, on April 30, 2023. All uses are freely permitted.",
        trademarkNotice = "The Berean Standard Bible has no trademark or copyright holder; it is " +
            "freely available for any use.",
    );

    companion object {
        val DEFAULT = ESV

        /** old cached preferences predate this feature and have no stored translation,
         *  treat that as ESV since it was the only option then. */
        fun fromNameOrDefault(name: String?): Translation =
            name?.let { stored -> entries.firstOrNull { it.name == stored } } ?: DEFAULT
    }
}
