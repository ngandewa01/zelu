package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

class DatesTest {
    @Test fun daysBetweenCountsWholeDays() {
        assertEquals(0, Dates.daysBetween("2026-10-28", "2026-10-28"))
        assertEquals(2, Dates.daysBetween("2026-10-28", "2026-10-30"))
        assertEquals(4, Dates.daysBetween("2026-10-28", "2026-11-01"))
        assertEquals(-1, Dates.daysBetween("2026-03-01", "2026-02-28"))
        assertEquals(365, Dates.daysBetween("2026-01-01", "2027-01-01"))
    }
}
