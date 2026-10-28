package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuSearchTest {
    // A few menu rows as the menu searches them: section, title, subtitle and extra words.
    private val rows = mapOf(
        "App lock" to "Backup and privacy App lock Ask for your phone lock when opening Zelu pin fingerprint password security",
        "Export" to "Backup and privacy Export spending (CSV) What's on screen now, to Downloads/Zelu csv excel spreadsheet download",
        "Payment alerts" to "Alerts Payment alerts A note for every payment or balance message notification",
        "Dark theme" to "Look Dark theme night light",
        "Erase" to "Backup and privacy Erase everything Delete all Zelu data from this phone delete wipe reset",
        "Back up now" to "Backup and privacy Back up now Save everything to Downloads/Zelu save copy",
        "Monthly budget" to "Money Monthly budget Not set limit spending",
        "Categories" to "Money Categories What counts as spending, and income categories counted ignore income",
    )

    private fun find(q: String): List<String> {
        val t = MenuSearch.tokens(q)
        val scored = rows.map { (k, v) -> k to MenuSearch.score(t, v) }
        val all = scored.filter { it.second == t.size && t.isNotEmpty() }
        return (all.ifEmpty { scored.filter { it.second > 0 }.sortedByDescending { it.second } }).map { it.first }
    }

    @Test fun plainWords() {
        assertEquals(listOf("Back up now"), find("back up").filter { it == "Back up now" })
        assertTrue("Monthly budget" in find("budget"))
    }

    @Test fun everydayWords() {
        assertTrue("App lock" in find("pin"))
        assertTrue("Export" in find("excel"))
        assertTrue("Erase" in find("delete"))
        assertTrue("Categories" in find("salary"))
        assertTrue("Monthly budget" in find("limit"))
    }

    @Test fun typos() {
        assertTrue("Payment alerts" in find("notifcation"))
        assertTrue("Erase" in find("dlete"))
        assertTrue("Monthly budget" in find("buget"))
        assertTrue("Dark theme" in find("drak theme"))
    }

    @Test fun wordsThatMeanNothingFindNothing() {
        assertEquals(emptyList<String>(), find("zzzz"))
        assertEquals(emptyList<String>(), find("the"))
    }
}
