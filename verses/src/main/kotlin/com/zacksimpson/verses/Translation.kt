package com.zacksimpson.verses

/** Which backend a [Translation] is fetched through — lets [VerseFetcher] branch on the
 *  API shape (ESV vs. YouVersion) instead of on each individual translation, so adding a
 *  new YouVersion-backed translation only means adding an enum case below. Also carries
 *  the caching/usage note shown in Copyright Info, since that's a property of the
 *  backend (how it's fetched/cached), not of any one translation — NIV and NASB share
 *  the same YouVersion note rather than repeating it.
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
    );

    companion object {
        val DEFAULT = ESV

        /** Cached preference values predate this feature and won't have a stored
         *  translation at all — treat that as ESV since it was the only option then. */
        fun fromNameOrDefault(name: String?): Translation =
            name?.let { stored -> entries.firstOrNull { it.name == stored } } ?: DEFAULT
    }
}
