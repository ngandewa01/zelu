package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmsTest {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = Dates.IST }
    private fun at(s: String) = fmt.parse(s)!!.time
    private fun show(ms: Long) = fmt.format(ms)

    @Test fun laterTodayWhenStillAhead() {
        assertEquals("2026-10-29 09:00:00", show(Alarms.nextAt(9 * 60, at("2026-10-29 08:00:00"))))
        assertEquals("2026-10-29 21:30:00", show(Alarms.nextAt(21 * 60 + 30, at("2026-10-29 09:00:00"))))
    }

    @Test fun tomorrowOncePassed() {
        assertEquals("2026-10-30 09:00:00", show(Alarms.nextAt(9 * 60, at("2026-10-29 09:00:00"))))
        assertEquals("2026-10-30 09:00:00", show(Alarms.nextAt(9 * 60, at("2026-10-29 23:59:00"))))
        assertEquals("2026-11-01 09:00:00", show(Alarms.nextAt(9 * 60, at("2026-10-31 10:00:00"))))
    }

    // An alarm delivered a few seconds early must not set itself again for the same minute.
    @Test fun aFiringNeverRepeatsWithinTheMinute() {
        assertEquals("2026-10-30 09:00:00", show(Alarms.nextAt(9 * 60, at("2026-10-29 08:59:30"))))
    }
}
