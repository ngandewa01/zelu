package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

// Which payments Home marks, and which cards the Recent strip shows first.
class AttentionTest {
    private fun row(id: String, amount: Double, kind: Kind = Kind.EXPENSE, category: String = "Food") =
        Row(id, "2026-10-03", Math.round(amount * 100), Dir.DEBIT, "Shop $id", null, "", "HDFCBK", "HDFC 9065", kind, category, 0L)

    @Test
    fun anUnsortedSpendIsMarkedButASortedOneIsNot() {
        val a = row("a", 450.0, category = "Unknown")
        val b = row("b", 212.0)
        val c = row("c", 90.0, kind = Kind.INCOME, category = "Unknown")
        assertEquals(mapOf("a" to Attention.UNKNOWN), Analytics.attention(listOf(a, b, c), emptyList(), emptyList()))
    }

    @Test
    fun aDuplicateOutranksARefundWhichOutranksAnUnsortedSpend() {
        val a = row("a", 450.0, category = "Unknown")
        val b = row("b", 450.0, category = "Unknown")
        val r = row("r", 2491.0, kind = Kind.REFUND, category = "Refund")
        val p = row("p", 3599.0)
        val marks = Analytics.attention(listOf(a, b, r, p), listOf(r to p), listOf(a to b))
        assertEquals(Attention.DUPLICATE, marks["a"])
        assertEquals(Attention.DUPLICATE, marks["b"])
        assertEquals(Attention.REFUND, marks["r"])
        assertEquals(null, marks["p"])
    }
}
