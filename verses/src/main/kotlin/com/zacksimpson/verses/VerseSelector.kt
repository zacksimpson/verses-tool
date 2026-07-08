package com.zacksimpson.verses

import java.time.LocalDate

internal object VerseSelector {
    fun referenceForDate(date: LocalDate = LocalDate.now()): String {
        val index = (date.dayOfYear - 1) % VerseCatalog.references.size
        return VerseCatalog.references[index]
    }
}
