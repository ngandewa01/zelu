package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

// Amounts are grouped the Indian way: thousands, then lakhs and crores in pairs.
class MoneyFormatTest {
    @Test
    fun indianGrouping() {
        assertEquals("0", groupIndian(0))
        assertEquals("999", groupIndian(999))
        assertEquals("1,000", groupIndian(1000))
        assertEquals("57,067", groupIndian(57067))
        assertEquals("1,76,217", groupIndian(176217))
        assertEquals("12,34,56,789", groupIndian(123456789))
        assertEquals("-1,25,000", groupIndian(-125000))
        assertEquals("₹1,76,217", money(176216.6))
        assertEquals("₹23,418.77", moneyPaise(23418.77))
        assertEquals("₹1,00,000.05", moneyPaise(100000.05))
    }
}
