package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

class TxnEntityTest {
    private fun row(amount: Double, counterparty: String? = "SWIGGY", ref: String? = "629145872310") =
        Row("ref-629145872310", "2026-10-18", amount, "debit", counterparty, ref, "Sent Rs.349.00 ...", "AX-HDFCBK-S",
            "HDFC 5997", "expense", "Food", 1792305000000L)

    // Every amount a message can carry (two decimals at most) comes back exactly as stored.
    @Test fun amountsSurviveAsPaise() {
        for (a in listOf(349.0, 0.29, 0.01, 1249.5, 79377.5, 2104.18, 150000.0, 12345678.99)) {
            val e = TxnEntity.of(row(a))
            assertEquals(Math.round(a * 100), e.amountPaise)
            assertEquals(a, e.toRow().amount, 0.0)
        }
        assertEquals(29L, TxnEntity.of(row(0.29)).amountPaise)  // 0.29 * 100 is 28.999... in floating point
    }

    @Test fun everyFieldRoundTrips() {
        val r = row(349.0)
        assertEquals(r, TxnEntity.of(r).toRow())
        val bare = row(10.0, counterparty = null, ref = null).copy(ms = 0)
        assertEquals(bare, TxnEntity.of(bare).toRow())
    }
}
