package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The sample data must read like real use: every made-up SMS is understood, and spends land in real categories.
class DemoDataTest {
    @Test
    fun everySampleMessageIsRead() {
        val msgs = Demo.messages(1792464000000L)   // 2026-10-20
        assertTrue("about two months of messages", msgs.size in 120..260)
        val problems = ArrayList<String>()
        val cats = HashMap<String, Int>()
        for (m in msgs) {
            if (!Parser.isFinancialSender(m.address) || Parser.hasSuspiciousLink(m.body, m.address)) { problems.add("skipped: " + m.body.take(60)); continue }
            val t = Parser.parseSms(m.body, m.date, m.address)
            if (t == null) {
                if (Parser.parseReminder(m.body, m.date, m.address) == null) problems.add("not read: " + m.body.take(60))
                continue
            }
            val r = Classifier.classify(t, emptyList())
            if (r.category == "Unknown") problems.add("Unknown category: " + m.body.take(60))
            cats[r.category] = (cats[r.category] ?: 0) + 1
        }
        println("Sample categories: $cats")
        assertEquals(problems.joinToString("\n"), 0, problems.size)
        for (c in listOf("Food", "Groceries", "Transport", "Shopping", "Fuel", "Bills", "Income", "Card bill", "Refund"))
            assertTrue("sample shows $c", (cats[c] ?: 0) > 0)
    }
}
