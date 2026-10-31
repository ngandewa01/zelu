package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

class PatternMathTest {
    private fun row(date: String, rupees: Long, raw: String = "Rs $rupees paid") =
        Row("id$date$rupees", date, rupees * 100, Dir.DEBIT, "Shop", null, raw, "HDFCBK", "HDFC 1234", Kind.EXPENSE, "Food", 0L)

    @Test fun thirdsSplitTheMonth() {
        val v = PatternMath.thirds(listOf(row("2026-10-03", 100), row("2026-10-10", 50), row("2026-10-15", 30), row("2026-10-31", 20)))
        assertEquals(listOf(150.0, 30.0, 20.0), v.toList())
    }

    @Test fun smallSpendsAreUnderTheLimit() {
        val s = PatternMath.small(listOf(row("2026-10-03", 100), row("2026-10-04", 150), row("2026-10-05", 5000)))
        assertEquals(2, s.count)
        assertEquals(250.0, s.total, 0.001)
        assertEquals(125.0, s.average, 0.001)
    }

    @Test fun timeOfDayUsesTheTimeInTheMessage() {
        val v = PatternMath.timeOfDay(listOf(
            row("2026-10-03", 100, "paid at 08:15 AM"), row("2026-10-03", 200, "paid at 7:41 PM"), row("2026-10-03", 300, "paid at 11:30 PM")))
        assertEquals(listOf(100.0, 0.0, 200.0, 300.0), v.toList())
    }
}
