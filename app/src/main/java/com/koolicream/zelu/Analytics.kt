package com.koolicream.zelu

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

object Dates {
    val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private fun sdf(p: String) = SimpleDateFormat(p, Locale.US).apply { timeZone = IST }

    fun today(): String = sdf("yyyy-MM-dd").format(Date())
    fun fmtMs(ms: Long): String = sdf("yyyy-MM-dd").format(Date(ms))
    fun todayMonth(): String = sdf("yyyy-MM").format(Date())
    fun dayOfMonth(): Int = GregorianCalendar(IST).get(Calendar.DAY_OF_MONTH)

    fun parse(dateStr: String): Calendar =
        GregorianCalendar(IST).apply { time = sdf("yyyy-MM-dd").parse(dateStr) ?: Date() }

    fun shiftDays(dateStr: String, delta: Int): String {
        val c = parse(dateStr); c.add(Calendar.DAY_OF_MONTH, delta); return sdf("yyyy-MM-dd").format(c.time)
    }

    // Whole days from one yyyy-MM-dd to another (negative when "to" is earlier).
    fun daysBetween(from: String, to: String): Int =
        Math.round((parse(to).timeInMillis - parse(from).timeInMillis) / 86_400_000.0).toInt()

    // First day of the month that is (n-1) months before monthStr (yyyy-MM).
    fun firstDayMonthsAgo(monthStr: String, n: Int): String {
        val (y, mo) = monthStr.split("-").map { it.toInt() }
        val c = GregorianCalendar(IST); c.clear(); c.set(y, mo - 1, 1); c.add(Calendar.MONTH, -(n - 1))
        return sdf("yyyy-MM-dd").format(c.time)
    }

    fun daysInMonth(monthStr: String): Int {
        val (y, mo) = monthStr.split("-").map { it.toInt() }
        val c = GregorianCalendar(IST); c.clear(); c.set(y, mo - 1, 1)
        return c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun shiftMonth(monthStr: String, delta: Int): String {
        val (y, mo) = monthStr.split("-").map { it.toInt() }
        val c = GregorianCalendar(IST); c.clear(); c.set(y, mo - 1, 1); c.add(Calendar.MONTH, delta)
        return SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = IST }.format(c.time)
    }

    fun monthLabel(monthStr: String): String {
        val (y, mo) = monthStr.split("-").map { it.toInt() }
        val names = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return names[mo - 1] + " '" + y.toString().takeLast(2)
    }

    fun monthLong(monthStr: String): String {
        val names = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")
        return names[monthStr.split("-")[1].toInt() - 1]
    }

    // Recent months ending at todayMonth, most recent last.
    fun recentMonths(todayMonth: String, n: Int): List<String> {
        val (y, mo) = todayMonth.split("-").map { it.toInt() }
        return (0 until n).map {
            val c = GregorianCalendar(IST); c.clear(); c.set(y, mo - 1, 1); c.add(Calendar.MONTH, -(n - 1 - it))
            SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = IST }.format(c.time)
        }
    }
}

object Analytics {

    data class Filter(
        val period: String,       // week|month|3m|6m|year|all|custom
        val month: String,        // yyyy-MM (used by month)
        val customFrom: String,
        val customTo: String,
        val account: String,      // "All" or an account label
        val type: String = "all", // all|expense|income
        val category: String = "All",
    )

    // Inclusive [start, end] date range for a filter.
    fun range(f: Filter): Pair<String, String> {
        val today = Dates.today()
        return when (f.period) {
            "week" -> Dates.shiftDays(today, -6) to today
            "month" -> "${f.month}-01" to "${f.month}-%02d".format(Dates.daysInMonth(f.month))
            "3m" -> Dates.firstDayMonthsAgo(Dates.todayMonth(), 3) to today
            "6m" -> Dates.firstDayMonthsAgo(Dates.todayMonth(), 6) to today
            "year" -> "${today.take(4)}-01-01" to "${today.take(4)}-12-31"
            "all" -> "0000-01-01" to "9999-12-31"
            "custom" -> f.customFrom to f.customTo
            else -> "${f.month}-01" to "${f.month}-31"
        }
    }

    fun periodLabel(f: Filter): String = when (f.period) {
        "week" -> "the last 7 days"
        "month" -> Dates.monthLong(f.month)
        "3m" -> "the last 3 months"
        "6m" -> "the last 6 months"
        "year" -> Dates.today().take(4)
        "all" -> "all time"
        "custom" -> "${f.customFrom} to ${f.customTo}"
        else -> Dates.monthLong(f.month)
    }

    fun filter(rows: List<Row>, f: Filter): List<Row> {
        val (start, end) = range(f)
        return rows.filter { r ->
            r.date in start..end &&
                (f.account == "All" || r.account == f.account) &&
                typeOk(r, f.type) &&
                catOk(r, f.category)
        }
    }

    private fun typeOk(r: Row, type: String): Boolean = when (type) {
        "expense" -> r.kind == "expense"
        "income" -> r.kind == "income" || r.kind == "refund"
        "ignore" -> r.kind == "ignore"
        // "Everything" hides ignored rows — pick "Ignored" to see and un-ignore them.
        else -> r.kind != "ignore"
    }

    private fun catOk(r: Row, cat: String): Boolean = when (cat) {
        "All" -> true
        "Unknown" -> r.kind == "expense" && r.category == "Unknown"
        else -> r.category == cat
    }

    data class Summary(val spent: Double, val income: Double, val net: Double, val pending: Int)

    fun summarize(list: List<Row>): Summary {
        var spent = 0.0; var income = 0.0; var pending = 0
        for (t in list) when (t.kind) {
            "expense" -> { spent += t.amount; if (t.category == "Unknown") pending++ }
            "income" -> income += t.amount
            "refund" -> if (t.category == "Refund") income += t.amount else spent -= t.amount  // paired refund nets the spend
        }
        return Summary(spent, income, income - spent, pending)
    }

    fun byCategory(list: List<Row>): List<Pair<String, Double>> {
        val m = LinkedHashMap<String, Double>()
        list.filter { it.kind == "expense" }.forEach { m[it.category] = (m[it.category] ?: 0.0) + it.amount }
        list.filter { it.kind == "refund" && it.category != "Refund" }.forEach { m[it.category] = (m[it.category] ?: 0.0) - it.amount }
        return m.entries.filter { it.value > 0.5 }.sortedByDescending { it.value }.map { it.key to it.value }
    }

    // Expense totals by category for one month/account — used for month-over-month deltas.
    fun categoryTotals(rows: List<Row>, month: String, account: String): Map<String, Double> {
        val m = HashMap<String, Double>()
        rows.filter { it.kind == "expense" && it.date.startsWith(month) && (account == "All" || it.account == account) }
            .forEach { m[it.category] = (m[it.category] ?: 0.0) + it.amount }
        rows.filter { it.kind == "refund" && it.category != "Refund" && it.date.startsWith(month) && (account == "All" || it.account == account) }
            .forEach { m[it.category] = (m[it.category] ?: 0.0) - it.amount }
        return m
    }

    data class Payee(val name: String, val amount: Double, val count: Int, val cats: List<String>)

    // What a normal day costs lately: the last 30 days of spending, without the big one-offs (rent, EMI,
    // a large purchase), which are judged against your own typical payment, not a fixed number.
    fun typicalDaily(list: List<Row>, today: String): Double {
        val from = Dates.shiftDays(today, -30)
        val amts = list.filter { it.kind == "expense" && it.date >= from && it.date <= today }.map { it.amount }
        if (amts.isEmpty()) return 0.0
        val med = amts.sorted()[amts.size / 2]
        val cap = (med * 3).coerceAtLeast(1.0)
        return amts.filter { it <= cap }.sum() / 30.0
    }

    // Many card SMS don't state the available limit. The last SMS that did is the anchor; what the card has
    // spent (or been paid back) on later days, in messages that carry no limit of their own, is applied on top,
    // so the available credit keeps falling as you spend. Returns net spent since the anchor (can be negative).
    fun cardUsedSince(rows: List<Row>, account: String, limit: Pair<Double, Long>?): Double {
        if (limit == null) return 0.0
        val anchor = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = Dates.IST }
            .format(java.util.Date(limit.second))
        var net = 0.0
        for (r in rows) {
            if (r.account != account || r.date <= anchor) continue
            if (Parser.limitFromTxn(r.raw) != null) continue
            net += if (r.direction == "debit") r.amount else -r.amount
        }
        return net
    }

    // Reference numbers named in a message: "UPI:756424637751", "UPI Ref no 123…", "Ref. no. 6085…", "reference number", "RRN", "UTR", "Txn id".
    private val REF_IN_TEXT = Regex("""\b(?:upi(?:\s*ref(?:erence)?)?|ref(?:erence)?|rrn|utr|txn\s*id|transaction\s*id|imps)\s*[:#.\-]*\s*(?:no\.?|number|id|#)?\s*[:#.\-]*\s*([A-Za-z]{0,4}\d{6,})""", RegexOption.IGNORE_CASE)
    private fun refsOf(r: Row): Set<String> {
        val s = HashSet<String>()
        r.ref?.takeIf { it.isNotBlank() }?.let { s.add(it.trim()) }
        REF_IN_TEXT.findAll(r.raw).forEach { s.add(it.groupValues[1]) }
        return s
    }

    // The balance a message reports after the payment ("Avl Bal Rs 1,200", "Wallet Bal: Rs.600"), the time it names, the payee.
    private val BAL_IN_TEXT = Regex("""\b(?:bal(?:ance)?|avl\.?\s*lmt|avl\.?\s*limit|available\s*limit)\s*(?:is|:|-)?\s*(?:rs\.?|inr|₹)?\s*([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private fun balanceIn(raw: String): Double? = BAL_IN_TEXT.find(raw)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    private val TIME_IN_TEXT = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)(?::([0-5]\d))?\b""")
    private fun timeIn(raw: String): String? = TIME_IN_TEXT.find(raw)?.value
    private fun payeeKey(n: String?): String = (n ?: "").lowercase().filter { it.isLetterOrDigit() }

    // How the money moved. One payment goes one way, so a UPI payment and an NEFT transfer of the same amount on the same
    // day are two payments, however alike the rest looks.
    private val CHANNELS = listOf(
        "upi" to Regex("""\bupi\b|\bvpa\b|@[a-z]{2,}\b""", RegexOption.IGNORE_CASE),
        "neft" to Regex("""\bneft\b""", RegexOption.IGNORE_CASE),
        "imps" to Regex("""\bimps\b""", RegexOption.IGNORE_CASE),
        "rtgs" to Regex("""\brtgs\b""", RegexOption.IGNORE_CASE),
        "mandate" to Regex("""\b(?:nach|ach|ecs|e-?mandate|auto-?pay)\b""", RegexOption.IGNORE_CASE),
        "atm" to Regex("""\batm\b|\bcash\s*(?:wdl|withdrawal)\b""", RegexOption.IGNORE_CASE),
        "cheque" to Regex("""\b(?:cheque|chq)\b""", RegexOption.IGNORE_CASE),
    )
    private fun channelsOf(raw: String): Set<String> = CHANNELS.filter { it.second.containsMatchIn(raw) }.map { it.first }.toSet()

    // Two payments that could be one payment reported twice. The same amount, account and day is only the start:
    // the same reference number says it is one payment, different reference numbers say two real payments, and with no
    // reference at all they must come from the same sender to be worth asking about. A different balance, time, payee
    // or way of paying (UPI, NEFT, IMPS...) says they are two.
    fun possibleDuplicates(rows: List<Row>, dismissed: Set<String>): List<Pair<Row, Row>> {
        val out = ArrayList<Pair<Row, Row>>()
        val groups = rows.filter { (it.kind == "expense" || it.kind == "income") && it.category != "EMI" && it.account.isNotBlank() }
            .groupBy { it.account + "|" + it.date + "|" + it.direction + "|" + Math.round(it.amount * 100) }
        for (g in groups.values) {
            if (g.size < 2) continue
            val sorted = g.sortedBy { it.id }
            val first = sorted[0]
            val fr = refsOf(first)
            for (i in 1 until sorted.size) {
                val b = sorted[i]
                val br = refsOf(b)
                val same = if (fr.isNotEmpty() && br.isNotEmpty()) fr.any { it in br }
                else first.source.isNotBlank() && first.source == b.source
                if (!same) continue
                // Anything that differs between the two messages says they are two payments.
                val bal1 = balanceIn(first.raw); val bal2 = balanceIn(b.raw)
                if (bal1 != null && bal2 != null && Math.abs(bal1 - bal2) > 0.005) continue
                val t1 = timeIn(first.raw); val t2 = timeIn(b.raw)
                if (t1 != null && t2 != null && t1 != t2) continue
                val p1 = payeeKey(first.counterparty); val p2 = payeeKey(b.counterparty)
                if (p1.isNotEmpty() && p2.isNotEmpty() && !p1.contains(p2) && !p2.contains(p1)) continue
                val c1 = channelsOf(first.raw); val c2 = channelsOf(b.raw)
                if (c1.isNotEmpty() && c2.isNotEmpty() && c1.none { it in c2 }) continue
                if ((first.id + "|" + b.id) in dismissed) continue
                out.add(first to b)
            }
        }
        return out.sortedByDescending { it.first.date }
    }

    /**
     * The same bank account written with a different number of digits in different messages ("3076", "83076",
     * "883076") is one account. A label whose number is the end of a longer number of the same bank joins that account.
     * A label with no bank name joins only when exactly one bank has such a longer number. Returns old label -> main label.
     */
    fun autoMerge(counts: Map<String, Int>): Map<String, String> {
        fun plain(a: String) = a.isNotBlank() && !a.contains(" CC") && !a.startsWith("CC ") && !a.contains(" Loan ") && !a.contains("FASTag")
        fun num(a: String) = a.trim().substringAfterLast(' ').takeIf { t -> t.length >= 2 && t.all { it.isDigit() } }
        fun bank(a: String) = if (a.trim().contains(' ')) a.trim().substringBeforeLast(' ') else ""
        val labels = counts.keys.filter { plain(it) && num(it) != null }
        val step = HashMap<String, String>()
        // Pass 1: numbers of four or more digits that are the end of a longer number.
        for (a in labels) {
            val n = num(a)!!
            if (n.length < 4) continue
            val targets = labels.filter { b -> b != a && num(b)!!.length > n.length && num(b)!!.endsWith(n) && (bank(a).isEmpty() || bank(a) == bank(b)) }
            if (targets.isEmpty()) continue
            if (bank(a).isEmpty() && targets.map { bank(it) }.distinct().size > 1) continue
            step[a] = targets.maxByOrNull { counts[it] ?: 0 }!!
        }
        fun root(a: String): String { var cur = a; var hops = 0; while (step.containsKey(cur) && hops++ < 4) cur = step.getValue(cur); return cur }
        // Pass 2: a very short tail ("76") joins only when exactly one account of that bank ends the same way.
        for (a in labels) {
            val n = num(a)!!
            if (n.length >= 4 || bank(a).isEmpty()) continue
            val roots = labels.filter { b -> b != a && num(b)!!.length > n.length && num(b)!!.endsWith(n) && bank(b) == bank(a) }.map { root(it) }.distinct()
            if (roots.size == 1) step[a] = roots[0]
        }
        val out = HashMap<String, String>()
        for (a in step.keys) { val r = root(a); if (r != a) out[a] = r }
        return out
    }

    fun byPayee(list: List<Row>): List<Payee> {
        val amt = LinkedHashMap<String, Double>()
        val cnt = LinkedHashMap<String, Int>()
        val cats = LinkedHashMap<String, LinkedHashSet<String>>()
        list.filter { it.kind == "expense" }.forEach { t ->
            val n = t.counterparty?.takeIf { it.isNotBlank() } ?: t.category
            amt[n] = (amt[n] ?: 0.0) + t.amount
            cnt[n] = (cnt[n] ?: 0) + 1
            cats.getOrPut(n) { LinkedHashSet() }.add(t.category)
        }
        return amt.entries.sortedByDescending { it.value }
            .map { Payee(it.key, it.value, cnt[it.key] ?: 1, cats[it.key]?.toList() ?: emptyList()) }
    }

    // Spend per day-of-month for the calendar (index 1..daysInMonth).
    fun dailyTotals(allRows: List<Row>, month: String, account: String): DoubleArray {
        val dim = Dates.daysInMonth(month)
        val arr = DoubleArray(dim + 1)
        allRows.filter { it.kind == "expense" && it.date.startsWith(month) && (account == "All" || it.account == account) }
            .forEach { val d = it.date.substring(8, 10).toInt(); if (d in 1..dim) arr[d] += it.amount }
        return arr
    }

    fun spentByAccount(list: List<Row>, accounts: List<String>): List<Pair<String, Double>> =
        accounts.filter { it != "All" }.map { a ->
            a to list.filter { it.account == a && it.kind == "expense" }.sumOf { it.amount }
        }.filter { it.second > 0 }

    fun lent(list: List<Row>) = list.filter { it.kind == "lent" }
    fun borrowed(list: List<Row>) = list.filter { it.kind == "borrowed" }
    // Investments (PF, NPS, mutual funds) — tracked on their own, not spend or income.
    fun investments(list: List<Row>) = list.filter { it.kind == "invest" }.newestFirst()

    data class Insight(val topCat: String, val topCatAmt: Double, val topCatPct: Int,
                       val biggestName: String, val biggestAmt: Double,
                       val overBy: Double?)

    fun insight(list: List<Row>, f: Filter, budget: Double = 0.0): Insight? {
        val expenses = list.filter { it.kind == "expense" }
        if (expenses.isEmpty()) return null
        val cats = byCategory(list)
        val spent = expenses.sumOf { it.amount }
        val top = cats.first()
        val biggest = expenses.maxByOrNull { it.amount }!!
        val overBy = if (budget > 0 && f.period == "month" && f.month == Dates.todayMonth()) {
            spent - budget * Dates.dayOfMonth() / Dates.daysInMonth(f.month)
        } else null
        return Insight(top.first, top.second, (top.second / spent * 100).toInt(),
            biggest.counterparty?.takeIf { it.isNotBlank() } ?: biggest.category, biggest.amount, overBy)
    }

    // Top N expenses (by amount) in the list — for highlighting big spends.
    fun bigSpendIds(list: List<Row>, n: Int = 3): Set<String> =
        list.filter { it.kind == "expense" }.sortedByDescending { it.amount }.take(n).map { it.id }.toSet()

    // Cumulative spend by day for a month (for the pace chart), up to `upto`.
    fun runningTotals(allRows: List<Row>, month: String, account: String, upto: Int): DoubleArray {
        val daily = dailyTotals(allRows, month, account)
        val out = DoubleArray(upto + 1)
        var run = 0.0
        for (d in 1..upto) { run += daily[d]; out[d] = run }
        return out
    }

    // Spending + income per month for the three-month bars.
    fun monthlySpendIncome(allRows: List<Row>, months: List<String>, account: String): List<Triple<String, Double, Double>> =
        months.map { m ->
            val inM = allRows.filter { it.date.startsWith(m) && (account == "All" || it.account == account) }
            val spent = inM.filter { it.kind == "expense" }.sumOf { it.amount } -
                inM.filter { it.kind == "refund" && it.category != "Refund" }.sumOf { it.amount }
            val income = inM.filter { it.kind == "income" || (it.kind == "refund" && it.category == "Refund") }.sumOf { it.amount }
            Triple(m, spent, income)
        }

    // "Heads up" items for a month: categories well above your own usual, and one-off big spends.
    // Returns (title, detail) pairs, at most three.
    fun unusual(all: List<Row>, month: String): List<Pair<String, String>> {
        fun inr(x: Double) = money(x)
        val skip = setOf("EMI", "Rent", "Investment", "Transfer", "Card bill", "Refund", "Income")
        val out = ArrayList<Triple<Double, String, String>>()
        val prev = (1..3).map { Dates.shiftMonth(month, -it) }
        val cur = categoryTotals(all, month, "All")
        val prevTotals = prev.map { categoryTotals(all, it, "All") }
        for ((c, v) in cur) {
            if (c in skip || v < 1000) continue
            val vals = prevTotals.map { it[c] ?: 0.0 }.filter { it > 0 }
            if (vals.size < 2) continue
            val usual = vals.average()
            if (v > usual * 1.5 && v - usual >= 1000)
                out.add(Triple(v - usual, "$c is " + inr(v) + " this month", "Your usual is about " + inr(usual) + "."))
        }
        // One-off big spend: > 3x the typical single spend in that category over the last 90 days.
        val from = Dates.shiftDays(Dates.today(), -90)
        for (e in all.filter { it.kind == "expense" && it.date.startsWith(month) && it.amount >= 5000 && it.category !in skip }) {
            val past = all.filter { it.kind == "expense" && it.category == e.category && it.date >= from && it.date < month + "-01" }
                .map { it.amount }.sorted()
            if (past.size < 3) continue
            val median = past[past.size / 2]
            if (e.amount > median * 3)
                out.add(Triple(e.amount - median, inr(e.amount) + " at " + (e.counterparty?.takeIf { it.isNotBlank() } ?: e.category),
                    "A typical " + e.category + " spend is about " + inr(median) + "."))
        }
        return out.sortedByDescending { it.first }.take(3).map { it.second to it.third }
    }

    // ---- Spend-page insights ----

    data class Recurring(val name: String, val category: String, val amount: Double, val prev: Double?, val last: String)

    // A payee you pay about the same amount to, once a month, in at least 3 of the last 4 months.
    fun recurring(all: List<Row>, todayMonth: String): List<Recurring> {
        val months = (0..3).map { Dates.shiftMonth(todayMonth, -it) } // newest first
        val out = ArrayList<Recurring>()
        val ex = all.filter { it.kind == "expense" && it.date.take(7) in months && !it.counterparty.isNullOrBlank() }
        ex.groupBy { it.counterparty!!.trim().lowercase() }.forEach { (_, rs) ->
            val byMonth = rs.groupBy { it.date.take(7) }
            val present = months.filter { byMonth[it]?.size == 1 }
            if (present.size < 3) return@forEach
            if (months[0] !in present && months[1] !in present) return@forEach
            val amts = present.map { byMonth[it]!![0].amount }
            if (amts.maxOrNull()!! > amts.minOrNull()!! * 1.2) return@forEach
            val newest = byMonth[present[0]]!![0]
            out.add(Recurring(newest.counterparty!!.trim(), newest.category, newest.amount, amts.getOrNull(1), newest.date))
        }
        return out.sortedByDescending { it.amount }
    }

    // Committed costs, as opposed to everyday spending. Learned from your own data: payees that repeat monthly
    // at a steady amount (EMI, rent, subscriptions...), categories made up almost entirely of those, plus any
    // category you marked as fixed in Budgets. Nothing about specific names is written into the app.
    class FixedSet(val payees: Set<String>, val cats: Set<String>)

    fun fixedSet(all: List<Row>, todayMonth: String, plan: Map<String, Double>): FixedSet {
        val payees = recurring(all, todayMonth).map { it.name.trim().lowercase() }.toSet()
        val months = (0..3).map { Dates.shiftMonth(todayMonth, -it) }
        val recent = all.filter { it.kind == "expense" && it.date.take(7) in months }
        val derived = recent.groupBy { it.category }.filter { (_, rs) ->
            val tot = rs.sumOf { it.amount }
            tot > 0 && rs.filter { (it.counterparty ?: "").trim().lowercase() in payees }.sumOf { it.amount } >= 0.8 * tot
        }.keys.map { it.lowercase() }.toSet()
        return FixedSet(payees, derived + plan.keys.map { it.trim().lowercase() })
    }

    fun isFixed(r: Row, fs: FixedSet): Boolean =
        r.category.lowercase() in fs.cats || (r.counterparty ?: "").trim().lowercase() in fs.payees

    // Average spend per weekday (Mon=0 .. Sun=6) between two dates, counting each weekday's occurrences.
    fun weekdayAverages(list: List<Row>, from: String, to: String): DoubleArray {
        val sums = DoubleArray(7); val counts = IntArray(7)
        fun idx(d: String): Int = (Dates.parse(d).get(Calendar.DAY_OF_WEEK) + 5) % 7
        var d = from; var guard = 0
        while (d <= to && guard++ < 400) { counts[idx(d)]++; d = Dates.shiftDays(d, 1) }
        list.filter { it.kind == "expense" && it.date >= from && it.date <= to }.forEach { sums[idx(it.date)] += it.amount }
        return DoubleArray(7) { if (counts[it] > 0) sums[it] / counts[it] else 0.0 }
    }
}
