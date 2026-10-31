package com.koolicream.zelu

// The numbers behind the Analytics cards: when in the day, which payments, which part of the month, and small spends.
object PatternMath {
    /** 0 = Monday ... 6 = Sunday. */
    fun weekdayIndex(date: String): Int = (Dates.parse(date).get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7

    fun forWeekday(rows: List<Row>, i: Int): List<Row> = rows.filter { it.kind == Kind.EXPENSE && weekdayIndex(it.date) == i }

    fun partOfDay(r: Row): Int {
        val s = TxnTime.secondsOf(r)
        if (s < 0) return -1
        val h = s / 3600
        return when { h in 6..11 -> 0; h in 12..17 -> 1; h in 18..21 -> 2; else -> 3 }
    }

    fun forPartOfDay(rows: List<Row>, i: Int): List<Row> = rows.filter { it.kind == Kind.EXPENSE && partOfDay(it) == i }

    fun forThird(rows: List<Row>, i: Int): List<Row> = rows.filter {
        val d = it.date.drop(8).toIntOrNull() ?: return@filter false
        it.kind == Kind.EXPENSE && (if (d <= 10) 0 else if (d <= 20) 1 else 2) == i
    }

    const val SMALL_LIMIT = 200.0

    /** Spend in four parts of the day: 6-12, 12-18, 18-22 and 22-6. Payments with no time to go on are left out. */
    fun timeOfDay(rows: List<Row>): DoubleArray {
        val out = DoubleArray(4)
        for (r in rows) {
            if (r.kind != Kind.EXPENSE) continue
            val s = TxnTime.secondsOf(r)
            if (s < 0) continue
            val h = s / 3600
            out[when { h in 6..11 -> 0; h in 12..17 -> 1; h in 18..21 -> 2; else -> 3 }] += r.paise / 100.0
        }
        return out
    }

    fun biggest(rows: List<Row>, n: Int = 5): List<Row> =
        rows.filter { it.kind == Kind.EXPENSE }.sortedByDescending { it.paise }.take(n)

    /** Spend in days 1-10, 11-20 and 21-end. */
    fun thirds(rows: List<Row>): DoubleArray {
        val out = DoubleArray(3)
        for (r in rows) {
            if (r.kind != Kind.EXPENSE) continue
            val d = r.date.drop(8).toIntOrNull() ?: continue
            out[if (d <= 10) 0 else if (d <= 20) 1 else 2] += r.paise / 100.0
        }
        return out
    }

    class Small(val count: Int, val total: Double, val average: Double)

    fun small(rows: List<Row>): Small {
        val s = rows.filter { it.kind == Kind.EXPENSE && it.paise < SMALL_LIMIT * 100 }
        val total = s.sumOf { it.paise } / 100.0
        return Small(s.size, total, if (s.isEmpty()) 0.0 else total / s.size)
    }
}
