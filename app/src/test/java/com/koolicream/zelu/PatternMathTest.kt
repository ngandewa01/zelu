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

    @Test fun regularsArePaidMostMonths() {
        fun r(date: String, who: String, rs: Long) = Row("id$date$who", date, rs * 100, Dir.DEBIT, who, null, "x", "S", "A", Kind.EXPENSE, "Food", 0L)
        val rows = listOf(r("2026-07-02", "Milk", 1800), r("2026-08-02", "Milk", 1900), r("2026-10-02", "Milk", 2000),
            r("2026-10-05", "Croma", 18000), r("2026-09-05", "Maid", 3000), r("2026-10-05", "Maid", 3000))
        val reg = PatternMath.regulars(rows, "2026-10")
        assertEquals(listOf("Milk"), reg.map { it.name })
        assertEquals(1900.0, reg[0].perMonth, 0.01)
        assertEquals(3, reg[0].months)
    }

    @Test fun medianIgnoresOneBigMonth() {
        assertEquals(130.0, PatternMath.median(listOf(120.0, 130.0, 600.0)), 0.001)
        assertEquals(125.0, PatternMath.median(listOf(120.0, 130.0, 600.0, 110.0)), 0.001)
        assertEquals(0.0, PatternMath.median(emptyList()), 0.001)
    }
}
